package org.lskk.lumen.persistence.rabbitmq;

import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.lskk.lumen.core.LumenChannel;
import org.lskk.lumen.core.util.AsError;
import org.lskk.lumen.core.util.ToJson;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.persistence.service.FactServiceImpl;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Inject;

/**
 * Created by ceefour on 06/10/2015.
 */
@Component // FIXME: re-enable this when refactoring complete
@Profile("daemonApp")
public class FactRouter extends RouteBuilder {
    @Inject
    private FactService factService;
    @Inject
    private ToJson toJson;
    @Inject
    private AsError asError;
    @Inject
    private PlatformTransactionManager txMgr;

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&queue=" + LumenChannel.PERSISTENCE_FACT.key() + "&routingKey=" + LumenChannel.PERSISTENCE_FACT.key())
                .to("log:IN." + LumenChannel.PERSISTENCE_FACT.key() + "?showHeaders=true&showAll=true&multiline=true")
                .process(it -> {
                    final FactRequest factRequest = toJson.getMapper().readValue(it.getIn().getBody(byte[].class), FactRequest.class);
                    it.getIn().setHeader("CamelBeanMethodName", factRequest.getOperation().name());
                    it.getIn().setBody(factRequest);
                })
                .bean(factService)
                .bean(toJson)
                .to("log:OUT." + LumenChannel.PERSISTENCE_FACT.key() + "?showAll=true&multiline=true");

//        final String agentId = "arkan";
//                    final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
//                    final String switchArg = inBodyJson.path("@type").asText();
//                    if ("FindAllQuery".equals("FindAllQuery")) {
//                        final FindAllQuery findAllQuery = toJson.getMapper().convertValue(inBodyJson, FindAllQuery.class);
//                        final String classAbbrevRef = Optional.ofNullable(RdfUtils.getPREFIX_MAP().abbreviate(findAllQuery.getClassRef())).orElse(findAllQuery.getClassRef());
//                        final Resources<IndexedResource> resources = new TransactionTemplate(txMgr).execute(tx -> {
//                            final String cypher = "MATCH (e:Resource) -[:rdf_type*]-> (:Resource {href: {classAbbrevRef}}) RETURN e LIMIT {itemsPerPage}";
//                            final Map<String, Object> params = new HashMap<>();
//                            log.debug("Querying using {}: {}", params, cypher);
//                            final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
//                            try {
//                                final List<Node> rsList = FluentIterable.from(rs).transform(x -> (Node) x.get("e")).toList();
//                                log.debug("{} rows in result set for {}: {}", rsList.size(), classAbbrevRef, rsList);
//                                final List<IndexedResource> indexedResources = rsList.stream().map(it2 -> {
//                                    final IndexedResource indexedRes = new IndexedResource();
//                                    indexedRes.setHref((String) it2.getProperty("href"));
//                                    indexedRes.setPrefLabel((String) it2.getProperty("prefLabel"));
//                                    indexedRes.setIsPreferredMeaningOf((String) it2.getProperty("isPreferredMeaningOf"));
//                                    return indexedRes;
//                                }).collect(Collectors.toList());
//                                return new Resources<>(indexedResources);
//                            } finally {
//                                rs.finish();
//                            }
//                        });
//                        it.getIn().setBody(resources);
//                    } else if ("CypherQuery".equals(switchArg)) {
//                        final CypherQuery cypherQuery = toJson.getMapper().convertValue(inBodyJson, CypherQuery.class);
//                        final Resources<ResultRow> resources = new TransactionTemplate(txMgr).execute(tx -> {
//                            log.debug("Querying using {}: {}", cypherQuery.getParameters(), cypherQuery.getQuery());
//                            final Result<Map<String, Object>> rs = neo4j.query(cypherQuery.getQuery(), cypherQuery.getParameters());
//                            try {
//                                final ImmutableList<Map<String, Object>> rsList = ImmutableList.copyOf(rs);
//                                log.debug("{} rows in result set: {}", rsList.size(), rsList);
//                                final List<ResultRow> resultRowList = rsList.stream().map(row ->
//                                        new ResultRow(row.entrySet().stream().map(entry -> {
//                                                    if (entry.getValue() instanceof Node) {
//                                                        return new ResultCell(entry.getKey(), new Neo4jNode((Node) entry.getValue()));
//                                                    } else if (entry.getValue() instanceof Relationship) {
//                                                        return new ResultCell(entry.getKey(), new Neo4jRelationship((Relationship) entry.getValue()));
//                                                    } else {
//                                                        return new ResultCell(entry.getKey(), entry.getValue());
//                                                    }
//                                                }
//                                        ).collect(Collectors.toList()))).collect(Collectors.toList());
//                                return new Resources<>(resultRowList);
//                            } finally {
//                                rs.finish();
//                            }
//                        });
//                        it.getIn().setBody(resources);
//                    } else {
//                        throw new Exception("Unknown JSON message: " + inBodyJson);
//                    }

    }

}

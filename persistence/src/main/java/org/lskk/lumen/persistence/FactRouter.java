package org.lskk.lumen.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import org.apache.camel.builder.RouteBuilder;
import org.lskk.lumen.core.Channel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 06/10/2015.
 */
@Component
@Profile("daemon")
public class FactRouter extends RouteBuilder {
    @Inject
    private Neo4jTemplate neo4j;
    @Inject
    private ToJson toJson;
    @Inject
    private PlatformTransactionManager txMgr;

    @Override
    public void configure() throws Exception {
        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + Channel.PERSISTENCE_FACT.key("arkan"))
                .to("log:IN.persistence-fact?showHeaders=true&showAll=true&multiline=true")
                .process(it -> {
                    try {
                        final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                        final String switchArg = inBodyJson.path("@type").asText();
                        if ("FindAllQuery".equals("FindAllQuery")) {
                            final FindAllQuery findAllQuery = toJson.getMapper().convertValue(inBodyJson, FindAllQuery.class);
                            final String classAbbrevRef = Optional.ofNullable(RdfUtils.getPREFIX_MAP().abbreviate(findAllQuery.getClassRef())).orElse(findAllQuery.getClassRef());
                            final Resources<IndexedResource> resources = new TransactionTemplate(txMgr).execute(tx -> {
                                final String cypher = "MATCH (e:Resource) -[:rdf_type*]-> (:Resource {href: {classAbbrevRef}}) RETURN e LIMIT {itemsPerPage}";
                                final Map<String, Object> params = new HashMap<String, Object>();
                                log.debug("Querying using {}: {}", params, cypher);
                                final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                                try {
                                    final List<Node> rsList = FluentIterable.from(rs).transform(x -> (Node) x.get("e")).toList();
                                    log.debug("{} rows in result set for {}: {}", rsList.size(), classAbbrevRef, rsList);
                                    final List<IndexedResource> indexedResources = rsList.stream().map(it2 -> {
                                        final IndexedResource indexedRes = new IndexedResource();
                                        indexedRes.setHref((String) it2.getProperty("href"));
                                        indexedRes.setPrefLabel((String) it2.getProperty("prefLabel"));
                                        indexedRes.setIsPreferredMeaningOf((String) it2.getProperty("isPreferredMeaningOf"));
                                        return indexedRes;
                                    }).collect(Collectors.toList());
                                    return new Resources<>(indexedResources);
                                } finally {
                                    rs.finish();
                                }
                            });
                            it.getOut().setBody(resources);
                        } else if ("CypherQuery".equals(switchArg)) {
                            final CypherQuery cypherQuery = toJson.getMapper().convertValue(inBodyJson, CypherQuery.class);
                            final Resources<ResultRow> resources = new TransactionTemplate(txMgr).execute(tx -> {
                                log.debug("Querying using {}: {}", cypherQuery.getParameters(), cypherQuery.getQuery());
                                final Result<Map<String, Object>> rs = neo4j.query(cypherQuery.getQuery(), cypherQuery.getParameters());
                                try {
                                    final ImmutableList<Map<String, Object>> rsList = ImmutableList.copyOf(rs);
                                    log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                    final List<ResultRow> resultRowList = rsList.stream().map(row ->
                                            new ResultRow(row.entrySet().stream().map(entry -> {
                                                        if (entry.getValue() instanceof Node) {
                                                            return new ResultCell((String) entry.getKey(), new Neo4jNode((Node) entry.getValue()));
                                                        } else if (entry.getValue() instanceof Relationship) {
                                                            return new ResultCell((String) entry.getKey(), new Neo4jRelationship((Relationship) entry.getValue()));
                                                        } else {
                                                            return new ResultCell((String) entry.getKey(), entry.getValue());
                                                        }
                                                    }
                                            ).collect(Collectors.toList()))).collect(Collectors.toList());
                                    return new Resources(resultRowList);
                                } finally {
                                    rs.finish();
                                }
                            });
                            it.getOut().setBody(resources);
                        } else {
                            throw new Exception("Unknown JSON message: " + inBodyJson);
                        }
                    } catch (Exception e) {
                        log.error("Cannot process: " + it.getIn().getBody(), e);
                        it.getOut().setBody(new Error(e));
                    }

                    it.getOut().getHeaders().put("rabbitmq.ROUTING_KEY",
                            Preconditions.checkNotNull(it.getIn().getHeaders().get("rabbitmq.REPLY_TO"),
                                    "\"rabbitmq.REPLY_TO\" header must be specified, found headers: %s", it.getIn().getHeaders()));
                    it.getOut().getHeaders().put("rabbitmq.EXCHANGE_NAME", "");
                })
                .bean(toJson)
                .to("rabbitmq://localhost/dummy?connectionFactory=#amqpConnFactory&autoDelete=false")
                .to("log:OUT.persistence-fact?showAll=true&multiline=true");
    }

}

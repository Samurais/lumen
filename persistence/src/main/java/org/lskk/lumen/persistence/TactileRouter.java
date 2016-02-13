package org.lskk.lumen.persistence;

import com.google.common.collect.ImmutableMap;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.lskk.lumen.core.util.AsError;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Created by ceefour on 06/10/2015.
 */
//@Component // FIXME: re-enable this when refactoring complete
@Profile("daemon")
public class TactileRouter extends RouteBuilder {
    @Inject
    private Neo4jOperations neo4j;
    @Inject
    private ToJson toJson;
    @Inject
    private AsError asError;
    @Inject
    private PlatformTransactionManager txMgr;

    @PostConstruct
    @Transactional
    public void init() {
        neo4j.query("CREATE INDEX ON :JournalTactile(name)", ImmutableMap.of());
        neo4j.query("CREATE INDEX ON :JournalTactile(dateCreated)", ImmutableMap.of());
    }

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        final String avatarId = "nao1";
//        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&queue=" + AvatarChannel.DATA_TACTILE.key(avatarId) + "&routingKey=" + AvatarChannel.DATA_TACTILE.key(avatarId))
//                .sample(1, TimeUnit.SECONDS)
//                .to("log:IN." + AvatarChannel.DATA_TACTILE.key(avatarId) + "?showHeaders=true&showAll=true&multiline=true")
//                .process(it -> {
//                    final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
//                    final TactileSetLegacy tactileSet = toJson.getMapper().convertValue(inBodyJson, TactileSetLegacy.class);
//                    final List<Node> nodesResult = new TransactionTemplate(txMgr).execute(tx -> {
//                        final DateTime now = new DateTime();
//                        final List<Node> nodes = new ArrayList<>();
//                        for (String tactileName : tactileSet.getNames()) {
//                            final Map<String, Object> props = new HashMap<>();
//                            final Node node = neo4j.createNode(props, ImmutableSet.of("JournalTactile"));
//                            nodes.add(node);
//                        }
//                        log.debug("Created {} JournalTactile(s) {} from {}", nodes, tactileSet);
//                        return nodes;
//                    });
//                    it.getOut().setBody(nodesResult.stream().map(Node::getId).collect(Collectors.toList()));
//                })
//                .bean(toJson)
//                .to("log:OUT." + AvatarChannel.DATA_TACTILE.key(avatarId) + "?showAll=true&multiline=true");
    }

}

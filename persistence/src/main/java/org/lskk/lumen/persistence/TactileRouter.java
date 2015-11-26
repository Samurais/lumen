package org.lskk.lumen.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.joda.time.DateTime;
import org.lskk.lumen.core.AvatarChannel;
import org.lskk.lumen.core.TactileSetLegacy;
import org.lskk.lumen.core.util.AsError;
import org.neo4j.graphdb.Node;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 06/10/2015.
 */
@Component
@Profile("daemon")
public class TactileRouter extends RouteBuilder {
    @Inject
    private Neo4jTemplate neo4j;
    @Inject
    private ToJson toJson;
    @Inject
    private AsError asError;
    @Inject
    private PlatformTransactionManager txMgr;

    @PostConstruct
    public void init() {
        new TransactionTemplate(txMgr).execute(tx -> {
            neo4j.query("CREATE INDEX ON :JournalTactile(name)", new LinkedHashMap()).finish();
            neo4j.query("CREATE INDEX ON :JournalTactile(dateCreated)", new LinkedHashMap()).finish();
            return null;
        });
    }

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        final String avatarId = "nao1";
        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&queue=" + AvatarChannel.DATA_TACTILE.key(avatarId) + "&routingKey=" + AvatarChannel.DATA_TACTILE.key(avatarId))
                .sample(1, TimeUnit.SECONDS)
                .to("log:IN." + AvatarChannel.DATA_TACTILE.key(avatarId) + "?showHeaders=true&showAll=true&multiline=true")
                .process(it -> {
                    final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                    final TactileSetLegacy tactileSet = toJson.getMapper().convertValue(inBodyJson, TactileSetLegacy.class);
                    final List<Node> nodesResult = new TransactionTemplate(txMgr).execute(tx -> {
                        final DateTime now = new DateTime();
                        final List<Node> nodes = new ArrayList<>();
                        for (String tactileName : tactileSet.getNames()) {
                            final Map<String, Object> props = new HashMap<>();
                            final Node node = neo4j.createNode(props, ImmutableSet.of("JournalTactile"));
                            nodes.add(node);
                        }
                        log.debug("Created {} JournalTactile(s) {} from {}", nodes, tactileSet);
                        return nodes;
                    });
                    it.getOut().setBody(nodesResult.stream().map(Node::getId).collect(Collectors.toList()));
                })
                .bean(toJson)
                .to("log:OUT." + AvatarChannel.DATA_TACTILE.key(avatarId) + "?showAll=true&multiline=true");
    }

}

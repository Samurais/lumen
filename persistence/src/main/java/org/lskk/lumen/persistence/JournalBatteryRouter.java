package org.lskk.lumen.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.joda.time.DateTime;
import org.lskk.lumen.core.AvatarChannel;
import org.lskk.lumen.core.BatteryState;
import org.lskk.lumen.core.util.AsError;
import org.neo4j.graphdb.Node;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by ceefour on 06/10/2015.
 */
@Component
@Profile("daemon")
public class JournalBatteryRouter extends RouteBuilder {
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
            neo4j.query("CREATE INDEX ON :JournalBatteryState(dateCreated)", new LinkedHashMap()).finish();
            return null;
        });
    }

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        final String avatarId = "nao1";
        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&queue=" + AvatarChannel.DATA_BATTERY.key(avatarId) + "&routingKey=" + AvatarChannel.DATA_BATTERY.key(avatarId))
                .sample(1, TimeUnit.SECONDS)
                .to("log:IN." + AvatarChannel.DATA_BATTERY.key(avatarId) + "?showHeaders=true&showAll=true&multiline=true")
                .process(it -> {
                    final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                    final BatteryState batteryState = toJson.getMapper().convertValue(inBodyJson, BatteryState.class);
                    final Node nodeResult = new TransactionTemplate(txMgr).execute(tx -> {
                        final DateTime now = new DateTime();
                        final Map<String, Object> props = new HashMap<String, Object>();
                        final Node node = neo4j.createNode(props, ImmutableSet.of("JournalBatteryState"));
                        log.debug("Created JournalBatteryState {} from {}", node, props);
                        return node;
                    });
                    it.getOut().setBody(nodeResult.getId());
                })
                .bean(toJson)
                .to("log:OUT." + AvatarChannel.DATA_BATTERY.key(avatarId) + "?showAll=true&multiline=true");
    }

}

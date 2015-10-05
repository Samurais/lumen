package org.lskk.lumen.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import org.apache.camel.builder.RouteBuilder;
import org.joda.time.DateTime;
import org.lskk.lumen.core.BatteryState;
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
public class BatteryRouter extends RouteBuilder {
    @Inject
    private Neo4jTemplate neo4j;
    @Inject
    private ToJson toJson;
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
        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.nao1.data.battery")
                .sample(1, TimeUnit.SECONDS)
                .to("log:IN.avatar.nao1.data.battery?showHeaders=true&showAll=true&multiline=true")
                .process(it -> {
                    try {
                        final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                        final BatteryState batteryState = toJson.getMapper().convertValue(inBodyJson, BatteryState.class);
                        new TransactionTemplate(txMgr).execute(tx -> {
                            final DateTime now = new DateTime();
                            final Map<String, Object> props = new HashMap<String, Object>();
                            final Node node = neo4j.createNode(props, ImmutableSet.of("JournalBatteryState"));
                            log.debug("Created JournalBatteryState {} from {}", node, props);
                            it.getOut().setBody(node.getId());
                            return null;
                        });
                    } catch (Exception e) {
                        log.error("Cannot process: " + it.getIn().getBody(), e);
                        it.getOut().setBody(new Error(e));
                    }
//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                })
                .bean(toJson)
                .to("log:OUT.avatar.nao1.data.battery?showAll=true&multiline=true");
    }

}

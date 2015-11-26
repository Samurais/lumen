package org.lskk.lumen.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.joda.time.DateTime;
import org.lskk.lumen.core.AvatarChannel;
import org.lskk.lumen.core.SonarState;
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
public class JournalSonarRouter extends RouteBuilder {

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
            neo4j.query("CREATE INDEX ON :JournalSonarState(dateCreated)", new LinkedHashMap()).finish();
            return null;
        });
    }

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        final String avatarId = "nao1";
        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&queue=" + AvatarChannel.DATA_SONAR.key(avatarId) + "&routingKey=" + AvatarChannel.DATA_SONAR.key(avatarId))
                .sample(1, TimeUnit.SECONDS)
                .to("log:IN." + AvatarChannel.DATA_SONAR.key(avatarId) + "?showHeaders=true&showAll=true&multiline=true")
                .process(it -> {
                    final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                    final SonarState sonarState = toJson.getMapper().convertValue(inBodyJson, SonarState.class);
                    final Node nodeResult = new TransactionTemplate(txMgr).execute(tx -> {
                        final DateTime now = new DateTime();
                        final Map<String, Object> props = new HashMap<String, Object>();
                        final Node node = neo4j.createNode(props, ImmutableSet.of("JournalSonarState"));
                        log.debug("Created JournalSonarState {} from {}", node, props);
                        return node;
                    });
                    it.getOut().setBody(nodeResult.getId());

//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                })
                .bean(toJson)
                .to("log:OUT." + AvatarChannel.DATA_SONAR.key(avatarId) + "?showAll=true&multiline=true");
    }

}

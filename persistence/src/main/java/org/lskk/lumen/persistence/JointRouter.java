package org.lskk.lumen.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.joda.time.DateTime;
import org.lskk.lumen.core.JointSetLegacy;
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
//@Component // FIXME: re-enable this when refactoring complete
@Profile("daemon")
public class JointRouter extends RouteBuilder {
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
            neo4j.query("CREATE INDEX ON :JournalJoint(name)", new LinkedHashMap()).finish();
            neo4j.query("CREATE INDEX ON :JournalJoint(dateCreated)", new LinkedHashMap()).finish();
            return null;
        });
    }

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&durable=false&autoDelete=true&routingKey=avatar.nao1.data.joint")
                .sample(1, TimeUnit.SECONDS)
                .to("log:IN.avatar.nao1.data.joint?showHeaders=true&showAll=true&multiline=true")
                .process(it -> {
                            final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                            final JointSetLegacy jointSet = toJson.getMapper().convertValue(inBodyJson, JointSetLegacy.class);
                            final List<Node> nodesResult = new TransactionTemplate(txMgr).execute(tx -> {
                                final DateTime now = new DateTime();
                                final List<Node> nodes = new ArrayList<>();
                                for (String jointName : jointSet.getNames()) {
                                    final Map<String, Object> props = new HashMap<String, Object>();
                                    final Node node = neo4j.createNode(props, new ArrayList<String>(Arrays.asList("JournalJoint")));
                                    nodes.add(node);
                                }
                                log.debug("Created {} JournalJoint(s) {} from {}", nodes, jointSet);
                                return nodes;
                            });
                            it.getOut().setBody(nodesResult.stream().map(Node::getId).collect(Collectors.toList()));
                        }
                )
                .bean(toJson)
                .to("log:OUT.avatar.nao1.data.joint?showAll=true&multiline=true");
    }

}

package org.lskk.lumen.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.joda.time.DateTime;
import org.lskk.lumen.core.*;
import org.lskk.lumen.core.util.AsError;
import org.neo4j.graphdb.Node;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
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
public class JournalRouter extends RouteBuilder {
    @Inject
    private Environment env;
    @Inject
    private Neo4jTemplate neo4j;
    @Inject
    private ToJson toJson;
    @Inject
    private AsError asError;
    @Inject
    private PlatformTransactionManager txMgr;

    @Override
    public void configure() throws Exception {
        final String mediaUploadPrefix = env.getRequiredProperty("media.upload.prefix");
        final String mediaDownloadPrefix = env.getRequiredProperty("media.download.prefix");
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + AvatarChannel.PERSISTENCE_JOURNAL.key("arkan"))
                .to("log:IN.persistence-journal?showHeaders=true&showAll=true&multiline=true")
                .process(it -> {
                    final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                    final String switchArg = inBodyJson.path("@type").asText();
                    if ("JournalImageQuery".equals(switchArg)) {
                        final JournalImageQuery journalImageQuery = toJson.getMapper().convertValue(inBodyJson, JournalImageQuery.class);
                        final Resources<ImageObject> resources = new TransactionTemplate(txMgr).execute(tx -> {
                            final Map<String, Object> params = new HashMap<String, Object>();
                            final String cypher = "MATCH (n:JournalImageObject) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                            log.debug("Querying: {} {}", cypher, params);
                            final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                            try {
                                final List<Node> rsList = FluentIterable.from(rs).transform(x -> (Node) x.get("n")).toList();
                                log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                return new Resources(rsList.stream().map(ix -> {
                                            final ImageObject imageObject = new ImageObject();
                                            imageObject.setDateCreated(Optional.ofNullable(ix.getProperty("dateCreated")).map(DateTime::new).orElse(null));
                                            imageObject.setDatePublished(Optional.ofNullable(it.getProperty("datePublished")).map(DateTime::new).orElse(null));
                                            imageObject.setDateModified(Optional.ofNullable(it.getProperty("dateModified")).map(DateTime::new).orElse(null));
                                            imageObject.setUploadDate(Optional.ofNullable(it.getProperty("uploadDate")).map(DateTime::new).orElse(null));
                                            final String upContentUrl = (String) it.getProperty("contentUrl");
                                            imageObject.setContentUrl(upContentUrl.replace(mediaUploadPrefix, mediaDownloadPrefix));
                                            imageObject.setContentSize((Long) it.getProperty("contentSize"));
                                            imageObject.setContentType((String) it.getProperty("contentType"));
                                            imageObject.setName((String) it.getProperty("name"));
                                            return imageObject;
                                        }
                                ).collect(Collectors.toList()));
                            } finally {
                                rs.finish();
                            }
                        });
                        it.getIn().setBody(resources);
                    } else if ("JournalJointQuery".equals(switchArg)) {
                        final JournalJointQuery journalJointQuery = toJson.getMapper().convertValue(inBodyJson, JournalJointQuery.class);
                        final Resources<JointState> resources = new TransactionTemplate(txMgr).execute(tx -> {
                            final Map<String, Object> params = new HashMap<>();
                            final String cypher = "MATCH (n:JournalJoint) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                            log.debug("Querying: {} {}", cypher, params);
                            final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                            try {
                                final List<Node> rsList = FluentIterable.from(rs).transform(x -> (Node) x.get("n")).toList();
                                log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                return new Resources(rsList.stream().map(ix -> {
                                    final JointState jointState = new JointState();
                                    jointState.setDateCreated(Optional.ofNullable(ix.getProperty("dateCreated")).map(DateTime::new).orElse(null));
                                    jointState.setName((String) ix.getProperty("name"));
                                    jointState.setAngle((Double) ix.getProperty("angle"));
                                    jointState.setStiffness((Double) ix.getProperty("stiffness"));
                                    return jointState;
                                }).collect(Collectors.toList()));
                            } finally {
                                rs.finish();
                            }
                        });
                        it.getIn().setBody(resources);
                    } else if ("JournalTactileQuery".equals(switchArg)) {
                        final JournalTactileQuery journalTactileQuery = toJson.getMapper().convertValue(inBodyJson, JournalTactileQuery.class);
                        final Resources<TactileState> resources = new TransactionTemplate(txMgr).execute(tx -> {
                            final Map<String, Object> params = new HashMap<String, Object>();
                            final String cypher = "MATCH (n:JournalTactile) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                            log.debug("Querying: {} {}", cypher, params);
                            final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                            try {
                                final List<Node> rsList = FluentIterable.from(rs).transform(x -> (Node) x.get("n")).toList();
                                log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                return new Resources(rsList.stream().map(ix -> {
                                    final TactileState tactileState = new TactileState();
                                    tactileState.setDateCreated(Optional.ofNullable(ix.getProperty("dateCreated")).map(DateTime::new).orElse(null));
                                    tactileState.setName((String) ix.getProperty("name"));
                                    tactileState.setValue((Double) ix.getProperty("value"));
                                    return tactileState;
                                }).collect(Collectors.toList()));
                            } finally {
                                rs.finish();
                            }
                        });
                        it.getIn().setBody(resources);
                    } else if ("JournalSonarQuery".equals(switchArg)) {
                        final JournalSonarQuery journalSonarQuery = toJson.getMapper().convertValue(inBodyJson, JournalSonarQuery.class);
                        final Resources<SonarState> resources = new TransactionTemplate(txMgr).execute(tx -> {
                            final Map<String, Object> params = new HashMap<>();
                            final String cypher = "MATCH (n:JournalSonarState) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                            log.debug("Querying: {} {}", cypher, params);
                            final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                            try {
                                final List<Node> rsList = FluentIterable.from(rs).transform(x -> (Node) x.get("n")).toList();
                                log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                return new Resources(rsList.stream().map(ix -> {
                                    final SonarState sonarState = new SonarState();
                                    sonarState.setDateCreated(Optional.ofNullable(ix.getProperty("dateCreated")).map(DateTime::new).orElse(null));
                                    sonarState.setLeftSensor((Double) ix.getProperty("leftSensor"));
                                    sonarState.setRightSensor((Double) ix.getProperty("rightSensor"));
                                    return sonarState;
                                }).collect(Collectors.toList()));
                            } finally {
                                rs.finish();
                            }
                        });
                        it.getIn().setBody(resources);
                    } else if ("JournalBatteryQuery".equals(switchArg)) {
                        final JournalBatteryQuery journalBatteryQuery = toJson.getMapper().convertValue(inBodyJson, JournalBatteryQuery.class);
                        final Resources<BatteryState> resources = new TransactionTemplate(txMgr).execute(tx -> {
                            final Map<String, Object> params = new HashMap<>();
                            final String cypher = "MATCH (n:JournalBatteryState) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                            log.debug("Querying: {} {}", cypher, params);
                            final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                            try {
                                final List<Node> rsList = FluentIterable.from(rs).transform(x -> (Node) x.get("n")).toList();
                                log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                return new Resources<>(rsList.stream().map(ix -> {
                                            final BatteryState batteryState = new BatteryState();
                                            batteryState.setDateCreated(Optional.ofNullable(ix.getProperty("dateCreated")).map(DateTime::new).orElse(null));
                                            batteryState.setPercentage((Double) ix.getProperty("percentage"));
                                            batteryState.setIsCharging((Boolean) ix.getProperty("isCharging"));
                                            batteryState.setIsPlugged((Boolean) ix.getProperty("isPlugged"));
                                            return batteryState;
                                        }
                                ).collect(Collectors.toList()));
                            } finally {
                                rs.finish();
                            }
                        });
                        it.getIn().setBody(resources);
                    } else {
                        throw new Exception("Unknown JSON message: " + inBodyJson);
                    }
                })
                .bean(toJson)
                .to("log:OUT.persistence-journal?showAll=true&multiline=true");
    }

}

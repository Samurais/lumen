package org.lskk.lumen.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.rabbitmq.client.ConnectionFactory;
import id.ac.itb.lumen.core.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 1/19/15.
 */
@Configuration
@Profile("daemon")
public class LumenRouteConfig {

    private static final Logger log = LoggerFactory.getLogger(LumenRouteConfig.class);

    @Inject
    protected Environment env;
    @Inject
    protected ToJson toJson;
    @Inject
    protected PlatformTransactionManager txMgr;
    @Inject
    protected Neo4jTemplate neo4j;

    @Bean
    public ConnectionFactory amqpConnFactory() {
        final ConnectionFactory connFactory = new ConnectionFactory();
        connFactory.setHost(env.getRequiredProperty("amqp.host"));
        connFactory.setUsername(env.getRequiredProperty("amqp.username"));
        connFactory.setPassword(env.getRequiredProperty("amqp.password"));
        return connFactory;
    }

    @Bean
    public RouteBuilder factRouteBuilder() {
        log.info("Initializing fact RouteBuilder");
        return new RouteBuilder() {
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

        };
    }

    @Bean
    public RouteBuilder journalRouteBuilder() {
        log.info("Initializing journal RouteBuilder");
        final String mediaUploadPrefix = env.getRequiredProperty("media.upload.prefix");
        final String mediaDownloadPrefix = env.getRequiredProperty("media.download.prefix");
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + Channel.PERSISTENCE_JOURNAL.key("arkan"))
                        .to("log:IN.persistence-journal?showHeaders=true&showAll=true&multiline=true")
                        .process(it -> {
                        try {
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
                                it.getOut().setBody(resources);
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
                                it.getOut().setBody(resources);
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
                                it.getOut().setBody(resources);
                            } else if ("JournalSonarQuery".equals(switchArg)) {
                                final JournalSonarQuery journalSonarQuery = toJson.getMapper().convertValue(inBodyJson, JournalSonarQuery.class);
                                final Resources<SonarState> resources = new TransactionTemplate(txMgr).execute(tx -> {
                                    final Map<String, Object> params = new HashMap<>();
                                    final String cypher = "MATCH (n:JournalSonarState) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                                    log.debug("Querying: {} {}", cypher, params);
                                    final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                                    try {
                                        final List<Node> rsList = FluentIterable.from(rs).transform(x -> (Node)x.get("n")).toList();
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
                                it.getOut().setBody(resources);
                            } else if ("JournalBatteryQuery".equals(switchArg)) {
                                final JournalBatteryQuery journalBatteryQuery = toJson.getMapper().convertValue(inBodyJson, JournalBatteryQuery.class);
                                final Resources<BatteryState> resources = new TransactionTemplate(txMgr).execute(tx -> {
                                    final Map<String, Object> params = new HashMap<>();
                                    final String cypher = "MATCH (n:JournalBatteryState) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                                    log.debug("Querying: {} {}", cypher, params);
                                    final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                                    try {
                                        final List<Node> rsList = FluentIterable.from(rs).transform(x -> (Node)x.get("n")).toList();
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
                                it.getOut().setBody(resources);
                            } else {
                                throw new Exception("Unknown JSON message: " + inBodyJson);
                            }
                        } catch (Exception e) {
                            log.error("Cannot process: " + it.getIn().getBody(), e);
                            it.getOut().setBody(new Error(e));
                        }

                        it.getOut().getHeaders().put("rabbitmq.ROUTING_KEY",
                                Preconditions.checkNotNull(
                                        it.getIn().getHeaders().get("rabbitmq.REPLY_TO"),
                                        "\"rabbitmq.REPLY_TO\" header must be specified, found headers: %s", it.getIn().getHeaders()));
                        it.getOut().getHeaders().put("rabbitmq.EXCHANGE_NAME", "");
                    })
                        .bean(toJson)
                        .to("rabbitmq://localhost/dummy?connectionFactory=#amqpConnFactory&autoDelete=false")
                        .to("log:OUT.persistence-journal?showAll=true&multiline=true");
            }

        };
    }

    @Bean
    public RouteBuilder imageRouteBuilder() {
        log.info("Initializing image RouteBuilder");

        final File mediaUploadPath = new File(env.getRequiredProperty("media.upload.path"));
        mediaUploadPath.mkdirs();
        final String mediaUploadPrefix = env.getRequiredProperty("media.upload.prefix");
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(4);
        map.put("image/jpeg", "jpg");
        map.put("image/png", "png");
        map.put("image/gif", "gif");
        map.put("image/bmp", "bmp");
        final LinkedHashMap<String, String> extensionMap = map;

        new TransactionTemplate(txMgr).execute(tx -> {
            neo4j.query("CREATE INDEX ON :JournalImageObject(dateCreated)", new LinkedHashMap()).finish();
            return null;
        });

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final String avatarId = "NAO";
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar." + avatarId + ".data.image").sample(1, TimeUnit.SECONDS).to("log:IN.avatar." + avatarId + ".data.image?showHeaders=true&showAll=true&multiline=true")
                        .process(it -> {
                        try {
                            final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                            final ImageObjectLegacy imageObject = toJson.getMapper().convertValue(inBodyJson, ImageObjectLegacy.class);
                            new TransactionTemplate(txMgr).execute(tx -> {
                                final DateTime now = new DateTime();// FIXME: NaoServer should send ISO formatted timestamp
                                final Map<String, Object> props = new HashMap<>();
                                final String contentType = Preconditions.checkNotNull(imageObject.getContentType(), "ImageObject.contentType must be specified");
                                final String upContentUrl = imageObject.getContentUrl();
                                if (upContentUrl != null && upContentUrl.startsWith("data:")) {
                                    final String base64 = StringUtils.substringAfter(upContentUrl, ",");
                                    final byte[] content = Base64.decodeBase64(base64);
                                    final String ext = Preconditions.checkNotNull(extensionMap.get(contentType),
                                            "Cannot get extension for MIME type \"%s\". Known MIME types: %s", contentType, extensionMap.keySet());
                                    // IIS disables double escaping, so avoid '+0700' in filename
                                    final String fileName = avatarId + "_journalimage_" + new DateTime(DateTimeZone.UTC).toString("yyyy-MM-dd'T'HH-mm-ssZ") + "." + ext;
                                    final File file = new File(mediaUploadPath, fileName);
                                    log.debug("Writing {} ImageObject to {} ...", contentType, file);
                                    try {
                                        FileUtils.writeByteArrayToFile(file, content);
                                    } catch (IOException e) {
                                        throw new RuntimeException("Cannot write to " + file, e);
                                    }
                                    props.put("contentUrl", mediaUploadPrefix + fileName);
                                } else {
                                    props.put("contentUrl", upContentUrl);
                                }

                                final Node node = neo4j.createNode(props, new ArrayList<String>(Arrays.asList("JournalImageObject")));
                                log.debug("Created JournalImageObject {} from {} {}", node, imageObject.getName(), now);
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
                        .to("log:OUT.avatar." + avatarId + ".data.image?showAll=true&multiline=true");
            }

        };
    }

    @Bean
    public RouteBuilder sonarRouteBuilder() {
        log.info("Initializing sonar RouteBuilder");

        new TransactionTemplate(txMgr).execute(tx -> {
            neo4j.query("CREATE INDEX ON :JournalSonarState(dateCreated)", new LinkedHashMap()).finish();
            return null;
        });

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.sonar")
                        .sample(1, TimeUnit.SECONDS).to("log:IN.avatar.NAO.data.sonar?showHeaders=true&showAll=true&multiline=true")
                        .process(it -> {
                        try {
                            final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                            final SonarState sonarState = toJson.getMapper().convertValue(inBodyJson, SonarState.class);
                            new TransactionTemplate(txMgr).execute(tx -> {
                                final DateTime now = new DateTime();
                                final Map<String, Object> props = new HashMap<String, Object>();
                                final Node node = neo4j.createNode(props, ImmutableSet.of("JournalSonarState"));
                                log.debug("Created JournalSonarState {} from {}", node, props);
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
                        .to("log:OUT.avatar.NAO.data.sonar?showAll=true&multiline=true");
            }

        };
    }

    @Bean
    public RouteBuilder batteryRouteBuilder() {
        log.info("Initializing battery RouteBuilder");

        new TransactionTemplate(txMgr).execute(tx -> {
            neo4j.query("CREATE INDEX ON :JournalBatteryState(dateCreated)", new LinkedHashMap()).finish();
            return null;
        });

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.battery")
                        .sample(1, TimeUnit.SECONDS)
                        .to("log:IN.avatar.NAO.data.battery?showHeaders=true&showAll=true&multiline=true")
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
                        .to("log:OUT.avatar.NAO.data.battery?showAll=true&multiline=true");
            }

        };
    }

    @Bean
    public RouteBuilder jointRouteBuilder() {
        log.info("Initializing joint RouteBuilder");

        new TransactionTemplate(txMgr).execute(tx -> {
            neo4j.query("CREATE INDEX ON :JournalJoint(name)", new LinkedHashMap()).finish();
            neo4j.query("CREATE INDEX ON :JournalJoint(dateCreated)", new LinkedHashMap()).finish();
            return null;
        });

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.joint")
                        .sample(1, TimeUnit.SECONDS)
                        .to("log:IN.avatar.NAO.data.joint?showHeaders=true&showAll=true&multiline=true")
                        .process(it -> {
                                    try {
                                        final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                                        final JointSetLegacy jointSet = toJson.getMapper().convertValue(inBodyJson, JointSetLegacy.class);
                                        new TransactionTemplate(txMgr).execute(tx -> {
                                            final DateTime now = new DateTime();
                                            final List<Node> nodes = new ArrayList<>();
                                            for (String jointName : jointSet.getNames()) {
                                                final Map<String, Object> props = new HashMap<String, Object>();
                                                final Node node = neo4j.createNode(props, new ArrayList<String>(Arrays.asList("JournalJoint")));
                                                nodes.add(node);
                                            }
                                            log.debug("Created {} JournalJoint(s) {} from {}", nodes, jointSet);
                                            it.getOut().setBody(nodes.stream().map(Node::getId).collect(Collectors.toList()));
                                            return null;
                                        });
                                    } catch (Exception e) {
                                        log.error("Cannot process: " + it.getIn().getBody(), e);
                                        it.getOut().setBody(new Error(e));
                                    }

//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                                }

                        )
                    .bean(toJson)
                        .to("log:OUT.avatar.NAO.data.joint?showAll=true&multiline=true");
            }

        };
    }

    @Bean
    public RouteBuilder tactileRouteBuilder() {
        log.info("Initializing tactile RouteBuilder");

        new TransactionTemplate(txMgr).execute(tx -> {
            neo4j.query("CREATE INDEX ON :JournalTactile(name)", new LinkedHashMap()).finish();
            neo4j.query("CREATE INDEX ON :JournalTactile(dateCreated)", new LinkedHashMap()).finish();
            return null;
        });

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.tactile")
                        .sample(1, TimeUnit.SECONDS)
                        .to("log:IN.avatar.NAO.data.tactile?showHeaders=true&showAll=true&multiline=true")
                        .process(it -> {
                                    try {
                                        final JsonNode inBodyJson = toJson.getMapper().readTree(it.getIn().getBody(byte[].class));
                                        final TactileSetLegacy tactileSet = toJson.getMapper().convertValue(inBodyJson, TactileSetLegacy.class);
                                        new TransactionTemplate(txMgr).execute(tx -> {
                                            final DateTime now = new DateTime();
                                            final List<Node> nodes = new ArrayList<>();
                                            for (String tactileName : tactileSet.getNames()) {
                                                final Map<String, Object> props = new HashMap<>();
                                                final Node node = neo4j.createNode(props, ImmutableSet.of("JournalTactile"));
                                                return nodes.add(node);
                                            }
                                            log.debug("Created {} JournalTactile(s) {} from {}", nodes, tactileSet);
                                            it.getOut().setBody(nodes.stream().map(Node::getId).collect(Collectors.toList()));
                                            return null;
                                        });
                                    } catch (Exception e) {
                                        log.error("Cannot process: " + it.getIn().getBody(), e);
                                        it.getOut().setBody(new Error(e));
                                    }
//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                                }
                        ).bean(toJson).to("log:OUT.avatar.NAO.data.tactile?showAll=true&multiline=true");
            }

        };
    }

}

package id.ac.itb.lumen.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.rabbitmq.client.ConnectionFactory;
import groovy.lang.Closure;
import groovy.transform.CompileStatic;
import id.ac.itb.lumen.core.*;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Configuration
@Profile("daemon")
public class LumenRouteConfig {
    @Bean
    public ConnectionFactory amqpConnFactory() {
        final ConnectionFactory connFactory = new ConnectionFactory();
        connFactory.setHost(env.getRequiredProperty("amqp.host"));
        connFactory.setUsername(env.getRequiredProperty("amqp.username"));
        connFactory.setPassword(env.getRequiredProperty("amqp.password"));
        return ((ConnectionFactory) (connFactory));
    }

    @Bean
    public RouteBuilder factRouteBuilder() {
        log.info("Initializing fact RouteBuilder");
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + Channel.PERSISTENCE_FACT.key("arkan")).to("log:IN.persistence-fact?showHeaders=true&showAll=true&multiline=true").process(new Closure<String>(this, this) {
                    public String doCall(Exchange it) {
                        try {
                            final JsonNode inBodyJson = toJson.getMapper().readTree(DefaultGroovyMethods.asType(it.getIn().getBody(), Byte[].class));
                            final String switchArg = inBodyJson.path("@type").asText();
                            if (StringGroovyMethods.isCase("FindAllQuery", getProperty("switchArg"))) {
                                final FindAllQuery findAllQuery = toJson.getMapper().convertValue(inBodyJson, FindAllQuery.class);
                                final String classAbbrevRef = Optional.ofNullable(RdfUtils.getPREFIX_MAP().abbreviate(findAllQuery.getClassRef())).orElse(findAllQuery.getClassRef());
                                final Resources resources = new TransactionTemplate(txMgr).execute(new Closure<Resources>(this, this) {
                                    public Resources doCall(TransactionStatus it) {
                                        final String cypher = "MATCH (e:Resource) -[:rdf_type*]-> (:Resource {href: {classAbbrevRef}}) RETURN e LIMIT {itemsPerPage}";
                                        final Map<String, Object> params = new Map<String, Object>() {
                                        };
                                        log.debug("Querying using {}: {}", params, cypher);
                                        final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                                        try {
                                            final List<Node> rsList = DefaultGroovyMethods.toList(DefaultGroovyMethods.collect(rs, new Closure<Node>(this, this) {
                                                public Node doCall(Map<String, Object> it) {
                                                    return DefaultGroovyMethods.asType(it.get("e"), Node.class);
                                                }

                                                public groovy.util.Node doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                            log.debug("{} rows in result set for {}: {}", rsList.size(), classAbbrevRef, rsList);
                                            return new Resources(DefaultGroovyMethods.collect(rsList, new Closure<IndexedResource>(this, this) {
                                                public IndexedResource doCall(Node it) {
                                                    final IndexedResource indexedRes = new IndexedResource();
                                                    indexedRes.setHref(it.getProperty("href"));
                                                    indexedRes.setPrefLabel(it.getProperty("prefLabel"));
                                                    indexedRes.setIsPreferredMeaningOf(it.getProperty("isPreferredMeaningOf"));
                                                    return indexedRes;
                                                }

                                                public IndexedResource doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                        } finally {
                                            rs.finish();
                                        }

                                    }

                                    public Resources doCall() {
                                        return doCall(null);
                                    }

                                });
                                it.getOut().setBody(resources);
                            } else if (StringGroovyMethods.isCase("CypherQuery", getProperty("switchArg"))) {
                                final CypherQuery cypherQuery = toJson.getMapper().convertValue(inBodyJson, CypherQuery.class);
                                final Resources resources = new TransactionTemplate(txMgr).execute(new Closure<Resources>(this, this) {
                                    public Resources doCall(TransactionStatus it) {
                                        log.debug("Querying using {}: {}", cypherQuery.getParameters(), cypherQuery.getQuery());
                                        final Result<Map<String, Object>> rs = neo4j.query(cypherQuery.getQuery(), cypherQuery.getParameters());
                                        try {
                                            final ImmutableList<Map<String, Object>> rsList = ImmutableList.copyOf(rs);
                                            log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                            return new Resources(DefaultGroovyMethods.collect(rsList, new Closure<ResultRow>(this, this) {
                                                public ResultRow doCall(Map<String, Object> it) {
                                                    return new ResultRow(DefaultGroovyMethods.collect(it, new Closure<ResultCell>(this, this) {
                                                        public ResultCell doCall(Object k, Object v) {
                                                            if (v instanceof Node) {
                                                                return new ResultCell((String) k, new Neo4jNode(DefaultGroovyMethods.asType(v, Node.class)));
                                                            } else if (v instanceof Relationship) {
                                                                return new ResultCell((String) k, new Neo4jRelationship(DefaultGroovyMethods.asType(v, Relationship.class)));
                                                            } else {
                                                                return new ResultCell((String) k, v);
                                                            }

                                                        }

                                                    }));
                                                }

                                                public ResultRow doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                        } finally {
                                            rs.finish();
                                        }

                                    }

                                    public Resources doCall() {
                                        return doCall(null);
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


                        it.getOut().getHeaders().put("rabbitmq.ROUTING_KEY", Preconditions.checkNotNull(it.getIn().getHeaders().get("rabbitmq.REPLY_TO"), "\"rabbitmq.REPLY_TO\" header must be specified, found headers: %s", it.getIn().getHeaders()));
                        return putAt0(it.getOut().getHeaders(), "rabbitmq.EXCHANGE_NAME", "");
                    }

                    public String doCall() {
                        return doCall(null);
                    }

                }).bean(toJson).to("rabbitmq://localhost/dummy?connectionFactory=#amqpConnFactory&autoDelete=false").to("log:OUT.persistence-fact?showAll=true&multiline=true");
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
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + Channel.PERSISTENCE_JOURNAL.key("arkan")).to("log:IN.persistence-journal?showHeaders=true&showAll=true&multiline=true").process(new Closure<String>(this, this) {
                    public String doCall(Exchange it) {
                        try {
                            final JsonNode inBodyJson = toJson.getMapper().readTree(DefaultGroovyMethods.asType(it.getIn().getBody(), Byte[].class));
                            final String switchArg = inBodyJson.path("@type").asText();
                            if (StringGroovyMethods.isCase("JournalImageQuery", getProperty("switchArg"))) {
                                final JournalImageQuery journalImageQuery = toJson.getMapper().convertValue(inBodyJson, JournalImageQuery.class);
                                final Resources resources = new TransactionTemplate(txMgr).execute(new Closure<Resources>(this, this) {
                                    public Resources doCall(TransactionStatus it) {
                                        final Map<String, Object> params = new Map<String, Object>() {
                                        };
                                        final String cypher = "MATCH (n:JournalImageObject) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                                        log.debug("Querying: {} {}", cypher, params);
                                        final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                                        try {
                                            final List<Node> rsList = DefaultGroovyMethods.toList(DefaultGroovyMethods.collect(rs, new Closure<Node>(this, this) {
                                                public Node doCall(Map<String, Object> it) {
                                                    return DefaultGroovyMethods.asType(it.get("n"), Node.class);
                                                }

                                                public groovy.util.Node doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                            log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                            return new Resources(DefaultGroovyMethods.collect(rsList, new Closure<ImageObject>(this, this) {
                                                public ImageObject doCall(Node it) {
                                                    final ImageObject imageObject = new ImageObject();
                                                    imageObject.setDateCreated(Optional.ofNullable(it.getProperty("dateCreated")).map(new Closure<DateTime>(this, this) {
                                                        public DateTime doCall(Object it) {
                                                            return new DateTime(it);
                                                        }

                                                        public DateTime doCall() {
                                                            return doCall(null);
                                                        }

                                                    }).orElse(null));
                                                    imageObject.setDatePublished(Optional.ofNullable(it.getProperty("datePublished")).map(new Closure<DateTime>(this, this) {
                                                        public DateTime doCall(Object it) {
                                                            return new DateTime(it);
                                                        }

                                                        public DateTime doCall() {
                                                            return doCall(null);
                                                        }

                                                    }).orElse(null));
                                                    imageObject.setDateModified(Optional.ofNullable(it.getProperty("dateModified")).map(new Closure<DateTime>(this, this) {
                                                        public DateTime doCall(Object it) {
                                                            return new DateTime(it);
                                                        }

                                                        public DateTime doCall() {
                                                            return doCall(null);
                                                        }

                                                    }).orElse(null));
                                                    imageObject.setUploadDate(Optional.ofNullable(it.getProperty("uploadDate")).map(new Closure<DateTime>(this, this) {
                                                        public DateTime doCall(Object it) {
                                                            return new DateTime(it);
                                                        }

                                                        public DateTime doCall() {
                                                            return doCall(null);
                                                        }

                                                    }).orElse(null));
                                                    final String upContentUrl = DefaultGroovyMethods.asType(it.getProperty("contentUrl"), String.class);
                                                    imageObject.setContentUrl(upContentUrl.replace(mediaUploadPrefix, mediaDownloadPrefix));
                                                    imageObject.setContentSize(DefaultGroovyMethods.asType(it.getProperty("contentSize"), Long.class));
                                                    imageObject.setContentType(it.getProperty("contentType"));
                                                    imageObject.setName(it.getProperty("name"));
                                                    return imageObject;
                                                }

                                                public ImageObject doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                        } finally {
                                            rs.finish();
                                        }

                                    }

                                    public Resources doCall() {
                                        return doCall(null);
                                    }

                                });
                                it.getOut().setBody(resources);
                            } else if (StringGroovyMethods.isCase("JournalJointQuery", getProperty("switchArg"))) {
                                final JournalJointQuery journalJointQuery = toJson.getMapper().convertValue(inBodyJson, JournalJointQuery.class);
                                final Resources resources = new TransactionTemplate(txMgr).execute(new Closure<Resources>(this, this) {
                                    public Resources doCall(TransactionStatus it) {
                                        final Map<String, Object> params = new Map<String, Object>() {
                                        };
                                        final String cypher = "MATCH (n:JournalJoint) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                                        log.debug("Querying: {} {}", cypher, params);
                                        final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                                        try {
                                            final List<Node> rsList = DefaultGroovyMethods.toList(DefaultGroovyMethods.collect(rs, new Closure<Node>(this, this) {
                                                public Node doCall(Map<String, Object> it) {
                                                    return DefaultGroovyMethods.asType(it.get("n"), Node.class);
                                                }

                                                public groovy.util.Node doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                            log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                            return new Resources(DefaultGroovyMethods.collect(rsList, new Closure<JointState>(this, this) {
                                                public JointState doCall(Node it) {
                                                    final JointState jointState = new JointState();
                                                    jointState.setDateCreated(Optional.ofNullable(it.getProperty("dateCreated")).map(new Closure<DateTime>(this, this) {
                                                        public DateTime doCall(Object it) {
                                                            return new DateTime(it);
                                                        }

                                                        public DateTime doCall() {
                                                            return doCall(null);
                                                        }

                                                    }).orElse(null));
                                                    jointState.setName(it.getProperty("name"));
                                                    jointState.setAngle(DefaultGroovyMethods.asType(it.getProperty("angle"), Double.class));
                                                    jointState.setStiffness(DefaultGroovyMethods.asType(it.getProperty("stiffness"), Double.class));
                                                    return jointState;
                                                }

                                                public JointState doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                        } finally {
                                            rs.finish();
                                        }

                                    }

                                    public Resources doCall() {
                                        return doCall(null);
                                    }

                                });
                                it.getOut().setBody(resources);
                            } else if (StringGroovyMethods.isCase("JournalTactileQuery", getProperty("switchArg"))) {
                                final JournalTactileQuery journalTactileQuery = toJson.getMapper().convertValue(inBodyJson, JournalTactileQuery.class);
                                final Resources resources = new TransactionTemplate(txMgr).execute(new Closure<Resources>(this, this) {
                                    public Resources doCall(TransactionStatus it) {
                                        final Map<String, Object> params = new Map<String, Object>() {
                                        };
                                        final String cypher = "MATCH (n:JournalTactile) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                                        log.debug("Querying: {} {}", cypher, params);
                                        final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                                        try {
                                            final List<Node> rsList = DefaultGroovyMethods.toList(DefaultGroovyMethods.collect(rs, new Closure<Node>(this, this) {
                                                public Node doCall(Map<String, Object> it) {
                                                    return DefaultGroovyMethods.asType(it.get("n"), Node.class);
                                                }

                                                public groovy.util.Node doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                            log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                            return new Resources(DefaultGroovyMethods.collect(rsList, new Closure<TactileState>(this, this) {
                                                public TactileState doCall(Node it) {
                                                    final TactileState tactileState = new TactileState();
                                                    tactileState.setDateCreated(Optional.ofNullable(it.getProperty("dateCreated")).map(new Closure<DateTime>(this, this) {
                                                        public DateTime doCall(Object it) {
                                                            return new DateTime(it);
                                                        }

                                                        public DateTime doCall() {
                                                            return doCall(null);
                                                        }

                                                    }).orElse(null));
                                                    tactileState.setName(it.getProperty("name"));
                                                    tactileState.setValue(DefaultGroovyMethods.asType(it.getProperty("value"), Double.class));
                                                    return tactileState;
                                                }

                                                public TactileState doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                        } finally {
                                            rs.finish();
                                        }

                                    }

                                    public Resources doCall() {
                                        return doCall(null);
                                    }

                                });
                                it.getOut().setBody(resources);
                            } else if (StringGroovyMethods.isCase("JournalSonarQuery", getProperty("switchArg"))) {
                                final JournalSonarQuery journalSonarQuery = toJson.getMapper().convertValue(inBodyJson, JournalSonarQuery.class);
                                final Resources resources = new TransactionTemplate(txMgr).execute(new Closure<Resources>(this, this) {
                                    public Resources doCall(TransactionStatus it) {
                                        final Map<String, Object> params = new Map<String, Object>() {
                                        };
                                        final String cypher = "MATCH (n:JournalSonarState) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                                        log.debug("Querying: {} {}", cypher, params);
                                        final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                                        try {
                                            final List<Node> rsList = DefaultGroovyMethods.toList(DefaultGroovyMethods.collect(rs, new Closure<Node>(this, this) {
                                                public Node doCall(Map<String, Object> it) {
                                                    return DefaultGroovyMethods.asType(it.get("n"), Node.class);
                                                }

                                                public groovy.util.Node doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                            log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                            return new Resources(DefaultGroovyMethods.collect(rsList, new Closure<SonarState>(this, this) {
                                                public SonarState doCall(Node it) {
                                                    final SonarState sonarState = new SonarState();
                                                    sonarState.setDateCreated(Optional.ofNullable(it.getProperty("dateCreated")).map(new Closure<DateTime>(this, this) {
                                                        public DateTime doCall(Object it) {
                                                            return new DateTime(it);
                                                        }

                                                        public DateTime doCall() {
                                                            return doCall(null);
                                                        }

                                                    }).orElse(null));
                                                    sonarState.setLeftSensor(DefaultGroovyMethods.asType(it.getProperty("leftSensor"), Double.class));
                                                    sonarState.setRightSensor(DefaultGroovyMethods.asType(it.getProperty("rightSensor"), Double.class));
                                                    return sonarState;
                                                }

                                                public SonarState doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                        } finally {
                                            rs.finish();
                                        }

                                    }

                                    public Resources doCall() {
                                        return doCall(null);
                                    }

                                });
                                it.getOut().setBody(resources);
                            } else if (StringGroovyMethods.isCase("JournalBatteryQuery", getProperty("switchArg"))) {
                                final JournalBatteryQuery journalBatteryQuery = toJson.getMapper().convertValue(inBodyJson, JournalBatteryQuery.class);
                                final Resources resources = new TransactionTemplate(txMgr).execute(new Closure<Resources>(this, this) {
                                    public Resources doCall(TransactionStatus it) {
                                        final Map<String, Object> params = new Map<String, Object>() {
                                        };
                                        final String cypher = "MATCH (n:JournalBatteryState) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}";
                                        log.debug("Querying: {} {}", cypher, params);
                                        final Result<Map<String, Object>> rs = neo4j.query(cypher, params);
                                        try {
                                            final List<Node> rsList = DefaultGroovyMethods.toList(DefaultGroovyMethods.collect(rs, new Closure<Node>(this, this) {
                                                public Node doCall(Map<String, Object> it) {
                                                    return DefaultGroovyMethods.asType(it.get("n"), Node.class);
                                                }

                                                public groovy.util.Node doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                            log.debug("{} rows in result set: {}", rsList.size(), rsList);
                                            return new Resources(DefaultGroovyMethods.collect(rsList, new Closure<BatteryState>(this, this) {
                                                public BatteryState doCall(Node it) {
                                                    final BatteryState batteryState = new BatteryState();
                                                    batteryState.setDateCreated(Optional.ofNullable(it.getProperty("dateCreated")).map(new Closure<DateTime>(this, this) {
                                                        public DateTime doCall(Object it) {
                                                            return new DateTime(it);
                                                        }

                                                        public DateTime doCall() {
                                                            return doCall(null);
                                                        }

                                                    }).orElse(null));
                                                    batteryState.setPercentage(DefaultGroovyMethods.asType(it.getProperty("percentage"), Double.class));
                                                    batteryState.setIsCharging(DefaultGroovyMethods.asType(it.getProperty("isCharging"), Boolean.class));
                                                    batteryState.setIsPlugged(DefaultGroovyMethods.asType(it.getProperty("isPlugged"), Boolean.class));
                                                    return batteryState;
                                                }

                                                public BatteryState doCall() {
                                                    return doCall(null);
                                                }

                                            }));
                                        } finally {
                                            rs.finish();
                                        }

                                    }

                                    public Resources doCall() {
                                        return doCall(null);
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


                        it.getOut().getHeaders().put("rabbitmq.ROUTING_KEY", Preconditions.checkNotNull(it.getIn().getHeaders().get("rabbitmq.REPLY_TO"), "\"rabbitmq.REPLY_TO\" header must be specified, found headers: %s", it.getIn().getHeaders()));
                        return putAt0(it.getOut().getHeaders(), "rabbitmq.EXCHANGE_NAME", "");
                    }

                    public String doCall() {
                        return doCall(null);
                    }

                }).bean(toJson).to("rabbitmq://localhost/dummy?connectionFactory=#amqpConnFactory&autoDelete=false").to("log:OUT.persistence-journal?showAll=true&multiline=true");
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

        new TransactionTemplate(txMgr).execute(new Closure<Void>(this, this) {
            public void doCall(Object tx) {
                neo4j.query("CREATE INDEX ON :JournalImageObject(dateCreated)", new LinkedHashMap()).finish();
            }

        });

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final String avatarId = "NAO";
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar." + avatarId + ".data.image").sample(1, TimeUnit.SECONDS).to("log:IN.avatar." + avatarId + ".data.image?showHeaders=true&showAll=true&multiline=true").process(new Closure<Object>(this, this) {
                    public Object doCall(final Exchange it) {
                        try {
                            final JsonNode inBodyJson = toJson.getMapper().readTree(DefaultGroovyMethods.asType(it.getIn().getBody(), Byte[].class));
                            final ImageObjectLegacy imageObject = toJson.getMapper().convertValue(inBodyJson, ImageObjectLegacy.class);
                            return new TransactionTemplate(txMgr).execute(new Closure<Long>(this, this) {
                                public Long doCall(Object tx) {
                                    final DateTime now = new DateTime();// FIXME: NaoServer should send ISO formatted timestamp
                                    final Map<String, Object> props = new Map<String, Object>() {
                                    };
                                    final String contentType = DefaultGroovyMethods.invokeMethod(com.google.common.base.Preconditions, "checkNotNull", new Object[]{imageObject.getContentType(), "ImageObject.contentType must be specified"});
                                    final String upContentUrl = imageObject.getContentUrl();
                                    if (upContentUrl != null && upContentUrl.startsWith("data:")) {
                                        final String base64 = StringUtils.substringAfter(upContentUrl, ",");
                                        final Byte[] content = Base64.decodeBase64(base64);
                                        final String ext = Preconditions.checkNotNull(extensionMap.get(contentType), "Cannot get extension for MIME type \"%s\". Known MIME types: %s", contentType, extensionMap.keySet());
                                        // IIS disables double escaping, so avoid '+0700' in filename
                                        final String fileName = avatarId + "_journalimage_" + new DateTime(DateTimeZone.UTC).toString("yyyy-MM-dd'T'HH-mm-ssZ") + "." + ext;
                                        final File file = new File(mediaUploadPath, fileName);
                                        log.debug("Writing {} ImageObject to {} ...", contentType, file);
                                        FileUtils.writeByteArrayToFile(file, content);
                                        props.put("contentUrl", mediaUploadPrefix + fileName);
                                    } else {
                                        props.put("contentUrl", upContentUrl);
                                    }

                                    final Node node = neo4j.createNode(props, new ArrayList<String>(Arrays.asList("JournalImageObject")));
                                    log.debug("Created JournalImageObject {} from {} {}", node, imageObject.getName(), now);
                                    return setBody(it.getOut(), node.getId());
                                }

                            });
                        } catch (Exception e) {
                            log.error("Cannot process: " + it.getIn().getBody(), e);
                            return setBody(it.getOut(), new Error(e));
                        }


//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                    }

                }).bean(toJson).to("log:OUT.avatar." + avatarId + ".data.image?showAll=true&multiline=true");
            }

        };
    }

    @Bean
    public RouteBuilder sonarRouteBuilder() {
        log.info("Initializing sonar RouteBuilder");

        new TransactionTemplate(txMgr).execute(new Closure<Void>(this, this) {
            public void doCall(Object tx) {
                neo4j.query("CREATE INDEX ON :JournalSonarState(dateCreated)", new LinkedHashMap()).finish();
            }

        });

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.sonar").sample(1, TimeUnit.SECONDS).to("log:IN.avatar.NAO.data.sonar?showHeaders=true&showAll=true&multiline=true").process(new Closure<Object>(this, this) {
                    public Object doCall(final Exchange it) {
                        try {
                            final JsonNode inBodyJson = toJson.getMapper().readTree(DefaultGroovyMethods.asType(it.getIn().getBody(), Byte[].class));
                            final SonarState sonarState = toJson.getMapper().convertValue(inBodyJson, SonarState.class);
                            return new TransactionTemplate(txMgr).execute(new Closure<Long>(this, this) {
                                public Long doCall(Object tx) {
                                    final DateTime now = new DateTime();
                                    final Map<String, Object> props = new Map<String, Object>() {
                                    };
                                    final Node node = neo4j.createNode(props, new ArrayList<String>(Arrays.asList("JournalSonarState")));
                                    log.debug("Created JournalSonarState {} from {}", node, props);
                                    return setBody(it.getOut(), node.getId());
                                }

                            });
                        } catch (Exception e) {
                            log.error("Cannot process: " + it.getIn().getBody(), e);
                            return setBody(it.getOut(), new Error(e));
                        }


//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                    }

                }).bean(toJson).to("log:OUT.avatar.NAO.data.sonar?showAll=true&multiline=true");
            }

        };
    }

    @Bean
    public RouteBuilder batteryRouteBuilder() {
        log.info("Initializing battery RouteBuilder");

        new TransactionTemplate(txMgr).execute(new Closure<Void>(this, this) {
            public void doCall(Object tx) {
                neo4j.query("CREATE INDEX ON :JournalBatteryState(dateCreated)", new LinkedHashMap()).finish();
            }

        });

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.battery").sample(1, TimeUnit.SECONDS).to("log:IN.avatar.NAO.data.battery?showHeaders=true&showAll=true&multiline=true").process(new Closure<Object>(this, this) {
                    public Object doCall(final Exchange it) {
                        try {
                            final JsonNode inBodyJson = toJson.getMapper().readTree(DefaultGroovyMethods.asType(it.getIn().getBody(), Byte[].class));
                            final BatteryState batteryState = toJson.getMapper().convertValue(inBodyJson, BatteryState.class);
                            return new TransactionTemplate(txMgr).execute(new Closure<Long>(this, this) {
                                public Long doCall(Object tx) {
                                    final DateTime now = new DateTime();
                                    final Map<String, Object> props = new Map<String, Object>() {
                                    };
                                    final Node node = neo4j.createNode(props, new ArrayList<String>(Arrays.asList("JournalBatteryState")));
                                    log.debug("Created JournalBatteryState {} from {}", node, props);
                                    return setBody(it.getOut(), node.getId());
                                }

                            });
                        } catch (Exception e) {
                            log.error("Cannot process: " + it.getIn().getBody(), e);
                            return setBody(it.getOut(), new Error(e));
                        }


//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                    }

                }).bean(toJson).to("log:OUT.avatar.NAO.data.battery?showAll=true&multiline=true");
            }

        };
    }

    @Bean
    public RouteBuilder jointRouteBuilder() {
        log.info("Initializing joint RouteBuilder");

        new TransactionTemplate(txMgr).execute(new Closure<Void>(this, this) {
            public void doCall(Object tx) {
                neo4j.query("CREATE INDEX ON :JournalJoint(name)", new LinkedHashMap()).finish();
                neo4j.query("CREATE INDEX ON :JournalJoint(dateCreated)", new LinkedHashMap()).finish();
            }

        });

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.joint").sample(1, TimeUnit.SECONDS).to("log:IN.avatar.NAO.data.joint?showHeaders=true&showAll=true&multiline=true").process(new Closure<Object>(this, this) {
                    public Object doCall(final Exchange it) {
                        try {
                            final JsonNode inBodyJson = toJson.getMapper().readTree(DefaultGroovyMethods.asType(it.getIn().getBody(), Byte[].class));
                            final JointSetLegacy jointSet = toJson.getMapper().convertValue(inBodyJson, JointSetLegacy.class);
                            return new TransactionTemplate(txMgr).execute(new Closure<List<Long>>(this, this) {
                                public List<Long> doCall(Object tx) {
                                    final DateTime now = new DateTime();
                                    final List<Node> nodes = new ArrayList<Node>();
                                    DefaultGroovyMethods.eachWithIndex(jointSet.getNames(), new Closure<Boolean>(this, this) {
                                        public Boolean doCall(Object jointName, Object i) {
                                            final Map<String, Object> props = new Map<String, Object>() {
                                            };
                                            final Node node = neo4j.createNode(props, new ArrayList<String>(Arrays.asList("JournalJoint")));
                                            return nodes.add(node);
                                        }

                                    });
                                    log.debug("Created {} JournalJoint(s) {} from {}", nodes, jointSet);
                                    return setBody(it.getOut(), DefaultGroovyMethods.collect(nodes, new Closure<Long>(this, this) {
                                        public Long doCall(Node it) {
                                            return it.getId();
                                        }

                                        public Long doCall() {
                                            return doCall(null);
                                        }

                                    }));
                                }

                            });
                        } catch (Exception e) {
                            log.error("Cannot process: " + it.getIn().getBody(), e);
                            return setBody(it.getOut(), new Error(e));
                        }


//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                    }

                }).bean(toJson).to("log:OUT.avatar.NAO.data.joint?showAll=true&multiline=true");
            }

        };
    }

    @Bean
    public RouteBuilder tactileRouteBuilder() {
        log.info("Initializing tactile RouteBuilder");

        new TransactionTemplate(txMgr).execute(new Closure<Void>(this, this) {
            public void doCall(Object tx) {
                neo4j.query("CREATE INDEX ON :JournalTactile(name)", new LinkedHashMap()).finish();
                neo4j.query("CREATE INDEX ON :JournalTactile(dateCreated)", new LinkedHashMap()).finish();
            }

        });

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.tactile").sample(1, TimeUnit.SECONDS).to("log:IN.avatar.NAO.data.tactile?showHeaders=true&showAll=true&multiline=true").process(new Closure<Object>(this, this) {
                    public Object doCall(final Exchange it) {
                        try {
                            final JsonNode inBodyJson = toJson.getMapper().readTree(DefaultGroovyMethods.asType(it.getIn().getBody(), Byte[].class));
                            final TactileSetLegacy tactileSet = toJson.getMapper().convertValue(inBodyJson, TactileSetLegacy.class);
                            return new TransactionTemplate(txMgr).execute(new Closure<List<Long>>(this, this) {
                                public List<Long> doCall(Object tx) {
                                    final DateTime now = new DateTime();
                                    final List<Node> nodes = new ArrayList<Node>();
                                    DefaultGroovyMethods.eachWithIndex(tactileSet.getNames(), new Closure<Boolean>(this, this) {
                                        public Boolean doCall(Object tactileName, Object i) {
                                            final Map<String, Object> props = new Map<String, Object>() {
                                            };
                                            final Node node = neo4j.createNode(props, new ArrayList<String>(Arrays.asList("JournalTactile")));
                                            return nodes.add(node);
                                        }

                                    });
                                    log.debug("Created {} JournalTactile(s) {} from {}", nodes, tactileSet);
                                    return setBody(it.getOut(), DefaultGroovyMethods.collect(nodes, new Closure<Long>(this, this) {
                                        public Long doCall(Node it) {
                                            return it.getId();
                                        }

                                        public Long doCall() {
                                            return doCall(null);
                                        }

                                    }));
                                }

                            });
                        } catch (Exception e) {
                            log.error("Cannot process: " + it.getIn().getBody(), e);
                            return setBody(it.getOut(), new Error(e));
                        }


//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                    }

                }).bean(toJson).to("log:OUT.avatar.NAO.data.tactile?showAll=true&multiline=true");
            }

        };
    }

    private static final Logger log = LoggerFactory.getLogger(LumenRouteConfig.class);
    @Inject
    protected Environment env;
    @Inject
    protected ToJson toJson;
    @Inject
    protected PlatformTransactionManager txMgr;
    @Inject
    protected Neo4jTemplate neo4j;

    private static <K, V, Value extends Value> Value putAt0(Map<K, V> propOwner, K key, Value value) {
        propOwner.put(key, value);
        return value;
    }

    private static <Value> Value setBody(Message propOwner, Value var1) {
        propOwner.setBody(var1);
        return var1;
    }
}

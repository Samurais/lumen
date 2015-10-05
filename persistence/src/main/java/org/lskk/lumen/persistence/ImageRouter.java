package org.lskk.lumen.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.lskk.lumen.core.ImageObjectLegacy;
import org.neo4j.graphdb.Node;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by ceefour on 06/10/2015.
 */
@Component
@Profile("daemon")
public class ImageRouter extends RouteBuilder {

    private LinkedHashMap<String, String> extensionMap;
    @Inject
    private Environment env;
    @Inject
    private Neo4jTemplate neo4j;
    @Inject
    private ToJson toJson;
    @Inject
    private PlatformTransactionManager txMgr;

    @PostConstruct
    public void init() {
        final File mediaUploadPath = new File(env.getRequiredProperty("media.upload.path"));
        mediaUploadPath.mkdirs();
        final String mediaUploadPrefix = env.getRequiredProperty("media.upload.prefix");
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(4);
        map.put("image/jpeg", "jpg");
        map.put("image/png", "png");
        map.put("image/gif", "gif");
        map.put("image/bmp", "bmp");
        extensionMap = map;

        new TransactionTemplate(txMgr).execute(tx -> {
            neo4j.query("CREATE INDEX ON :JournalImageObject(dateCreated)", new LinkedHashMap()).finish();
            return null;
        });
    }

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

}

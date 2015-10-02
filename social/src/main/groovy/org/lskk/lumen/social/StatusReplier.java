package org.lskk.lumen.social;

import com.google.common.collect.ImmutableMap;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by NADIA on 27/02/2015.
 */
public class StatusReplier {
    
    private static final Logger log = LoggerFactory.getLogger(StatusReplier.class);

    private ProducerTemplate producerTemplate;
    private String commentPostEndpoint;

    public StatusReplier(ProducerTemplate producerTemplate, String commentPostEndpoint) {
        this.producerTemplate = producerTemplate;
        this.commentPostEndpoint = commentPostEndpoint;
    }

    public void reply(UserComment comment) {
        log.debug("Replying to {}: {}", comment.getPostId(), comment.getMessage());
        final Map<String, Object> headers = ImmutableMap.of(
                "CamelFacebook.postId", comment.getPostId(),
                "CamelFacebook.message", comment.getMessage());
        producerTemplate.sendBodyAndHeaders(commentPostEndpoint, "", headers);
    }
    
}

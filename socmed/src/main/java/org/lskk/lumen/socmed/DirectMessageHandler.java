package org.lskk.lumen.socmed;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.social.TwitterAutoConfiguration;
import org.springframework.stereotype.Service;
import twitter4j.*;
import twitter4j.auth.AccessToken;

import javax.annotation.PreDestroy;

/**
 * Created by ceefour on 29/10/2015.
 */
@Service
public class DirectMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(DirectMessageHandler.class);
    private AccessToken accessToken;
    private TwitterAuthorization authorization;
    private TwitterStream stream;

    public void start() {
        stream = new TwitterStreamFactory().getInstance(accessToken);
        stream.addListener(new UserStreamAdapter() {
            @Override
            public void onDirectMessage(DirectMessage directMessage) {
                super.onDirectMessage(directMessage);
                log.info("Got direct message from @{}: {}", directMessage.getSenderScreenName(), directMessage);
            }

            @Override
            public void onStatus(Status status) {
                super.onStatus(status);
                log.debug("Got Twitter status: {}", status);
                if (StringUtils.startsWithIgnoreCase(status.getText(), "@" + authorization.getScreenName())) {
                    final String realMessage = StringUtils.removeStartIgnoreCase(status.getText(), "@" + authorization.getScreenName()).trim();
                    log.info("Got mention from @{}: {}", status.getUser().getScreenName(), realMessage);
                }
            }
        });
        stream.user();
    }

    @PreDestroy
    public void destroy() {
        if (stream != null) {
            stream.shutdown();
            stream = null;
        }
    }
}

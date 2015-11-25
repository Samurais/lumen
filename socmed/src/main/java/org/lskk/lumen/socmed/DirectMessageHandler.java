package org.lskk.lumen.socmed;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.social.TwitterAutoConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.Authorization;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Created by ceefour on 29/10/2015.
 */
@Service
@Profile("reasonerSocmedApp")
public class DirectMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(DirectMessageHandler.class);
    private TwitterAuthorization authorization;
    private TwitterStream stream;
    private Function<DirectMessage, String> onDirectMessage;
    private Function<Status, String> onMention;
    private Function<User, String> onFollowed;

    @Inject
    private TwitterStreamFactory twitterStreamFactory;

    public TwitterAuthorization getAuthorization() {
        return authorization;
    }

    public void setAuthorization(TwitterAuthorization authorization) {
        this.authorization = authorization;
    }

    public Function<DirectMessage, String> getOnDirectMessage() {
        return onDirectMessage;
    }

    public void setOnDirectMessage(Function<DirectMessage, String> onDirectMessage) {
        this.onDirectMessage = onDirectMessage;
    }

    public Function<Status, String> getOnMention() {
        return onMention;
    }

    public void setOnMention(Function<Status, String> onMention) {
        this.onMention = onMention;
    }

    public Function<User, String> getOnFollowed() {
        return onFollowed;
    }

    public void setOnFollowed(Function<User, String> onFollowed) {
        this.onFollowed = onFollowed;
    }

    @PostConstruct
    public void start() {
        log.info("Streaming Twitter user @{} {} ...", authorization.getScreenName(), authorization.getUserId());
        final AccessToken accessToken = new AccessToken(authorization.getAccessToken(), authorization.getAccessTokenSecret());
        stream = twitterStreamFactory.getInstance(accessToken);
        stream.addListener(new UserStreamAdapter() {
            @Override
            public void onDirectMessage(DirectMessage directMessage) {
                super.onDirectMessage(directMessage);
                if (authorization.getUserId() == directMessage.getSenderId()) {
                    log.debug("Skipping own DM {}", directMessage.getId());
                    return;
                }
                log.info("Got direct message from @{}: {}", directMessage.getSenderScreenName(), directMessage);
                if (DirectMessageHandler.this.onDirectMessage != null) {
                    DirectMessageHandler.this.onDirectMessage.apply(directMessage);
                }
            }

            @Override
            public void onStatus(Status status) {
                super.onStatus(status);
                if (authorization.getUserId() == status.getUser().getId()) {
                    log.debug("Skipping own status {}: {}", status.getUser().getId(), status.getText());
                    return;
                }
                log.debug("Got Twitter status: {}", status);
                if (StringUtils.startsWithIgnoreCase(status.getText(), "@" + authorization.getScreenName())) {
                    final String realMessage = StringUtils.removeStartIgnoreCase(status.getText(), "@" + authorization.getScreenName()).trim();
                    log.info("Got mention from @{}: {}", status.getUser().getScreenName(), realMessage);
                    if (DirectMessageHandler.this.onMention != null) {
                        DirectMessageHandler.this.onMention.apply(status);
                    }
                }
            }

            @Override
            public void onFollow(User source, User followedUser) {
                super.onFollow(source, followedUser);
                if (authorization.getUserId() == followedUser.getId()) {
                    log.info("Followed by @{} {}", source.getScreenName(), source.getId());
                    if (DirectMessageHandler.this.onFollowed != null) {
                        DirectMessageHandler.this.onFollowed.apply(source);
                    }
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

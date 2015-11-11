package org.lskk.lumen.reasoner.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.aiml.AimlService;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.lskk.lumen.socmed.AgentRepository;
import org.lskk.lumen.socmed.DirectMessageHandler;
import org.lskk.lumen.socmed.TwitterApp;
import org.lskk.lumen.socmed.TwitterAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.conf.PropertyConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/**
 * Created by ceefour on 29/10/2015.
 */
@Configuration
public class ReasonerTwitterConfig {

    private static final Logger log = LoggerFactory.getLogger(ReasonerTwitterConfig.class);
    public static final String APP_ID = "lumen";
    public static final String AGENT_ID = "arkan";

    @Inject
    private Environment env;
    @Inject
    private ObjectMapper mapper;
    @Inject
    private AimlService aimlService;

    @Bean
    public AgentRepository agentRepo() throws IOException {
        return new AgentRepository();
    }

    @Bean
    public TwitterApp twitterApp() throws IOException {
        return mapper.readValue(new File("config/" + APP_ID + ".TwitterApp.jsonld"), TwitterApp.class);
    }

    @Bean
    public TwitterAuthorization twitterAuthorization() throws IOException {
        return mapper.readValue(new File("config/agent/" + AGENT_ID + ".TwitterAuthorization.jsonld"), TwitterAuthorization.class);
    }

    @Bean
    public TwitterFactory twitterFactory() throws IOException {
        final Properties twitterProps = new Properties();
        twitterProps.put("oauth.consumerKey", twitterApp().getApiKey());
        twitterProps.put("oauth.consumerSecret", twitterApp().getApiSecret());
        final PropertyConfiguration twitterConf = new PropertyConfiguration(twitterProps);
        return new TwitterFactory(twitterConf);
    }

    @Bean
    public TwitterStreamFactory twitterStreamFactory() throws IOException {
        final Properties twitterProps = new Properties();
        twitterProps.put("oauth.consumerKey", twitterApp().getApiKey());
        twitterProps.put("oauth.consumerSecret", twitterApp().getApiSecret());
        final PropertyConfiguration twitterConf = new PropertyConfiguration(twitterProps);
        return new TwitterStreamFactory(twitterConf);
    }

    @Bean
    public DirectMessageHandler arkanDirectMessageHandler() throws IOException {
        final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(ReasonerTwitterConfig.class.getClassLoader());
        final DirectMessageHandler dmHandler = new DirectMessageHandler();
        dmHandler.setAuthorization(twitterAuthorization());
        final Twitter twitter = twitterFactory().getInstance(new AccessToken(twitterAuthorization().getAccessToken(), twitterAuthorization().getAccessTokenSecret()));
        dmHandler.setOnDirectMessage(dm -> {
            try {
                final AgentResponse resp = aimlService.process(Locale.US, dm.getText());
                final String replyDm;
                if (resp.getResponse() instanceof CommunicateAction) {
                    final CommunicateAction communicateAction = (CommunicateAction) resp.getResponse();
                    replyDm = StringUtils.abbreviate(communicateAction.getObject(), 10000);
                } else {
                    log.warn("AIML service cannot understand @{} {}: {}",
                            dm.getSenderScreenName(), dm.getSenderId(), dm.getText());
                    replyDm = "Sorry, I don't understand :(";
                }
                log.info("Replying DM to @{} {}: {}", dm.getSenderScreenName(), dm.getSenderId(), replyDm);
                twitter.sendDirectMessage(dm.getSenderId(), replyDm);
                return replyDm;
            } catch (Exception e) {
                final String stackTraceAsString = Throwables.getStackTraceAsString(e);
                try {
                    final String replyDm = StringUtils.abbreviate(stackTraceAsString, 255);
                    log.warn("Sending exception DM @{} {}: {}", dm.getSenderScreenName(), dm.getSenderId(), replyDm);
                    twitter.sendDirectMessage(dm.getSenderId(), replyDm);
                    return stackTraceAsString;
                } catch (TwitterException e1) {
                    throw new ReasonerException(e1, "Cannot DM @%s %s", dm.getSenderScreenName(), dm.getSenderId());
                }
            }
        });
        dmHandler.setOnMention(status -> {
            try {
                try {
                    twitter.friendsFollowers().createFriendship(status.getUser().getId());
                    log.info("Successfully followed @{} {}", status.getUser().getScreenName(), status.getUser().getId());
                } catch (TwitterException e) {
                    log.debug("Cannot follow @{} {}: {}", status.getUser().getScreenName(), status.getUser().getId(),
                            e.toString());
                }

                final String realMessage = StringUtils.removeStartIgnoreCase(status.getText(), "@" + twitterAuthorization().getScreenName()).trim();
                final AgentResponse resp = aimlService.process(Locale.US, realMessage);
                final CommunicateAction communicateAction = (CommunicateAction) resp.getResponse();
                final String replyTweet = "@" + status.getUser().getScreenName() + " " + StringUtils.abbreviate(communicateAction.getObject(), 140-20);
                final StatusUpdate replyStatus = new StatusUpdate(replyTweet);
                if (communicateAction.getImage() != null) {
                    final String url = communicateAction.getImage().getUrl();
                    Preconditions.checkArgument(url != null,
                            "CommunicateAction.ImageObject.url is required");
//                    Preconditions.checkArgument(url.startsWith("file:") || url.startsWith("classpath:"),
//                            "CommunicateAction.ImageObject.url only supports file: and classpath: schemes");
                    final Resource res = resourceResolver.getResource(url);
                    Preconditions.checkState(res.exists(), "%s does not exist", res);
                    replyStatus.setMedia(res.getFilename(), res.getInputStream());
//                    replyStatus.setMedia("");
                }
                replyStatus.setInReplyToStatusId(status.getId());
                twitter.tweets().updateStatus(replyStatus);
                return replyTweet;
            } catch (Exception e) {
                final String stackTraceAsString = Throwables.getStackTraceAsString(e);
                try {
                    final String replyTweet = "@" + status.getUser().getScreenName() + " " + StringUtils.abbreviate(stackTraceAsString, 140-20);
                    final StatusUpdate replyStatus = new StatusUpdate(replyTweet);
                    replyStatus.setInReplyToStatusId(status.getId());
                    twitter.tweets().updateStatus(replyStatus);
                    return stackTraceAsString;
                } catch (TwitterException e1) {
                    throw new ReasonerException(e1, "Cannot reply @%s %s", status.getUser().getScreenName(), status.getUser().getId());
                }
            }
        });
        dmHandler.setOnFollowed(follower -> {
            try {
                try {
                    twitter.friendsFollowers().createFriendship(follower.getId());
                    log.info("Successfully followed @{} {}", follower.getScreenName(), follower.getId());
                } catch (TwitterException e) {
                    log.debug("Cannot follow @{} {}: {}", follower.getScreenName(), follower.getId(),
                            e.toString());
                }

                final String replyDm = "Nice to know you, " + follower.getName() + "! I am Arkan Lumen. Can I help you?";
                log.info("Sending DM to @{} {}: {}", follower.getScreenName(), follower.getId(), replyDm);
                twitter.sendDirectMessage(follower.getId(), replyDm);
                return "";
            } catch (Exception e) {
                throw new ReasonerException(e, "Cannot DM @%s %s", follower.getScreenName(), follower.getId());
            }
        });
        return dmHandler;
    }

}

package org.lskk.lumen.reasoner.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imgur.ImgUr;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.core.ImageObject;
import org.lskk.lumen.reasoner.DroolsService;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.aiml.AimlService;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.lskk.lumen.reasoner.nlp.NaturalLanguage;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.lskk.lumen.reasoner.util.ImageObjectResolver;
import org.lskk.lumen.reasoner.ux.LogChannel;
import org.lskk.lumen.reasoner.visual.VisualCaptureRouter;
import org.lskk.lumen.socmed.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.conf.PropertyConfiguration;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

/**
 * Created by ceefour on 29/10/2015.
 */
@Profile("reasonerSocmedApp")
@Configuration
@Import(ImgurConfig.class)
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
    @Inject
    private ImgUr imgur;
    @Inject
    private ImgurConfig imgurConfig;
    @Inject
    private LogChannel logChannel; // FIXME: replace with proper twitter channel
    @Inject
    private ImageObjectResolver imageObjectResolver;
    @Inject @NaturalLanguage("en")
    private SentenceGenerator sentenceGenerator_en;
    @Inject @NaturalLanguage("id")
    private SentenceGenerator sentenceGenerator_id;
    @Inject
    private DroolsService droolsService;

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
        final DirectMessageHandler dmHandler = new DirectMessageHandler();
        dmHandler.setAuthorization(twitterAuthorization());
        final Twitter twitter = twitterFactory().getInstance(new AccessToken(twitterAuthorization().getAccessToken(), twitterAuthorization().getAccessTokenSecret()));
        dmHandler.setOnDirectMessage(dm -> {
            try {
                final TwitterDirectMessageChannel twitterDmChannel = new TwitterDirectMessageChannel(
                        sentenceGenerator_en, sentenceGenerator_id,
                        twitter, imageObjectResolver, imgurConfig, dm.getSenderScreenName());
                final AgentResponse resp = aimlService.process(Locale.US, dm.getText(), twitterDmChannel);
                droolsService.process(resp);
                String replyDm;
                if (resp.getCommunicateAction() instanceof CommunicateAction) {
                    final CommunicateAction communicateAction = (CommunicateAction) resp.getCommunicateAction();
                    // allow some characters for imgur URI
                    replyDm = StringUtils.abbreviate(communicateAction.getObject(), 10000 - 100);

                    if (communicateAction.getImage() != null) {
                        imageObjectResolver.resolve(communicateAction.getImage());
                        if (communicateAction.getImage().getContent() != null) {
                            final String imageId = imgurConfig.upload(
                                    ContentType.create(communicateAction.getImage().getContentType()),
                                    communicateAction.getImage().getContent(), communicateAction.getObject());
                            replyDm += " http://imgur.com/" + imageId;
                        }

                        // NOT WORKING!
//                        final UploadRequest uploadRequest = new UploadRequest.Builder()
//                                .withAccessToken(new Token(imgurConfig.getAccessToken(), ""))
//                                .withImageData(media)
//                                .withTitle(communicateAction.getObject())
//                                .build();
//                        final UploadResponse uploaded = imgur.call(uploadRequest);
//                        replyDm += " " + uploaded.getLinks().getImgurPage();
                    }
                } else {
                    log.warn("AIML service cannot understand @{} {}: {}",
                            dm.getSenderScreenName(), dm.getSenderId(), dm.getText());
                    replyDm = "Sorry, I don't understand :(";
                }
                if (!replyDm.isEmpty()) {
                    log.info("Replying DM to @{} {}: {}", dm.getSenderScreenName(), dm.getSenderId(), replyDm);
                    twitter.sendDirectMessage(dm.getSenderId(), replyDm);
                    return replyDm;
                } else {
                    return null;
                }
            } catch (Exception e) {
                log.error(String.format("Error DM @%s %s from: %s", dm.getSenderScreenName(), dm.getSenderId(), dm.getText()), e);
                final String stackTraceAsString = Throwables.getStackTraceAsString(e);
                try {
                    final String replyDm = StringUtils.abbreviate(stackTraceAsString, 1000);
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
                final TwitterMentionChannel twitterMentionChannel = new TwitterMentionChannel(
                        sentenceGenerator_en, sentenceGenerator_id,
                        twitter, imageObjectResolver, imgurConfig, status.getUser().getScreenName(), status.getId());
                final AgentResponse resp = aimlService.process(Locale.US, realMessage, twitterMentionChannel);
                droolsService.process(resp);
                final CommunicateAction communicateAction = (CommunicateAction) resp.getCommunicateAction();
                final boolean replyHasImage = communicateAction.getImage() != null;
                if (!communicateAction.getObject().isEmpty() || replyHasImage) {
                    final int maxReplyLength = 140 - (status.getUser().getScreenName().length() + 2)
                            - (replyHasImage ? 23 : 0);
                    final String replyTweet = "@" + status.getUser().getScreenName() + " " + StringUtils.abbreviate(communicateAction.getObject(), maxReplyLength);
                    final StatusUpdate replyStatus = new StatusUpdate(replyTweet);
                    if (replyHasImage) {
                        imageObjectResolver.resolve(communicateAction.getImage());
                        if (communicateAction.getImage().getContent() != null) {
                            final String imageId = imgurConfig.upload(
                                    ContentType.create(communicateAction.getImage().getContentType()),
                                    communicateAction.getImage().getContent(), communicateAction.getObject());
                            replyStatus.setMedia("image.jpg", new ByteArrayInputStream(communicateAction.getImage().getContent()));
                        }
                    }
                    replyStatus.setInReplyToStatusId(status.getId());
                    twitter.tweets().updateStatus(replyStatus);
                    return replyTweet;
                } else {
                    return null;
                }
            } catch (Exception e) {
                log.error("Error replying @" + status.getUser().getScreenName()+ "'s mention: " + status.getText(), e);
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

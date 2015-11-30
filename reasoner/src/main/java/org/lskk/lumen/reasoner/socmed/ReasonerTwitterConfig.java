package org.lskk.lumen.reasoner.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imgur.ImgUr;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.joda.time.Duration;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.core.ImageObject;
import org.lskk.lumen.core.SimpleTruthValue;
import org.lskk.lumen.core.SocialChannel;
import org.lskk.lumen.reasoner.DroolsService;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.aiml.AimlService;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.lskk.lumen.reasoner.nlp.NaturalLanguage;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.lskk.lumen.reasoner.social.SocialJournal;
import org.lskk.lumen.reasoner.social.SocialJournalRepository;
import org.lskk.lumen.reasoner.util.ImageObjectResolver;
import org.lskk.lumen.reasoner.ux.LogChannel;
import org.lskk.lumen.socmed.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.conf.PropertyConfiguration;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

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
    @Inject
    private SocialJournalRepository socialJournalRepo;

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
        final String avatarId = "anime1";
        final DirectMessageHandler dmHandler = new DirectMessageHandler();
        dmHandler.setAuthorization(twitterAuthorization());
        final Twitter twitter = twitterFactory().getInstance(new AccessToken(twitterAuthorization().getAccessToken(), twitterAuthorization().getAccessTokenSecret()));
        final Locale origLocale = Locale.US;
        dmHandler.setOnDirectMessage(dm -> {
            final long startTime = System.currentTimeMillis();
            try {
                final TwitterDirectMessageChannel twitterDmChannel = new TwitterDirectMessageChannel(
                        sentenceGenerator_en, sentenceGenerator_id,
                        twitter, imageObjectResolver, imgurConfig, dm.getSenderScreenName());
                final AgentResponse resp = aimlService.process(origLocale, dm.getText(), twitterDmChannel, null, false);
                droolsService.process(resp);
                String replyDm;
                if (!resp.getCommunicateActions().isEmpty()) {
                    //final CommunicateAction communicateAction = resp.getCommunicateActions();
                    final String communicateActionsObject = resp.getCommunicateActions().stream()
                            .map(CommunicateAction::getObject).collect(Collectors.joining(" ")).trim();
                    final Optional<CommunicateAction> communicateImg = resp.getCommunicateActions().stream()
                            .filter(it -> it.getImage() != null).findFirst();
                    // allow some characters for imgur URI
                    replyDm = StringUtils.abbreviate(communicateActionsObject, 10000 - 100);

                    if (communicateImg.isPresent()) {
                        imageObjectResolver.resolve(communicateImg.get().getImage());
                        if (communicateImg.get().getImage().getContent() != null) {
                            final String imageId = imgurConfig.upload(
                                    ContentType.create(communicateImg.get().getImage().getContentType()),
                                    communicateImg.get().getImage().getContent(), communicateImg.get().getObject());
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
                final String ret;
                if (!replyDm.isEmpty()) {
                    log.info("Replying DM to @{} {}: {}", dm.getSenderScreenName(), dm.getSenderId(), replyDm);
                    twitter.sendDirectMessage(dm.getSenderId(), replyDm);
                    ret = replyDm;
                } else {
                    ret = null;
                }

                final SocialJournal socialJournal = new SocialJournal();
                socialJournal.setFromResponse(origLocale, avatarId,
                        dm.getText(), SocialChannel.TWITTER,
                        resp, Duration.millis(System.currentTimeMillis() - startTime));
                socialJournalRepo.save(socialJournal);

                return ret;
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
            final long startTime = System.currentTimeMillis();
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
                final AgentResponse resp = aimlService.process(origLocale, realMessage, twitterMentionChannel, null, false);
                droolsService.process(resp);
                //final CommunicateAction communicateAction = resp.getCommunicateAction();
                final String communicateActionsObject = resp.getCommunicateActions().stream()
                        .map(CommunicateAction::getObject).collect(Collectors.joining(" ")).trim();
                final Optional<CommunicateAction> communicateImg = resp.getCommunicateActions().stream()
                        .filter(it -> it.getImage() != null).findFirst();
                final String ret;
                if (!Strings.isNullOrEmpty(communicateActionsObject) || communicateImg.isPresent()) {
                    final int maxReplyLength = 140 - (status.getUser().getScreenName().length() + 2)
                            - (communicateImg.isPresent() ? 23 : 0);
                    final String replyTweet = "@" + status.getUser().getScreenName() + " " + StringUtils.abbreviate(communicateActionsObject, maxReplyLength);
                    final StatusUpdate replyStatus = new StatusUpdate(replyTweet);
                    if (communicateImg.isPresent()) {
                        imageObjectResolver.resolve(communicateImg.get().getImage());
                        if (communicateImg.get().getImage().getContent() != null) {
                            final String imageId = imgurConfig.upload(
                                    ContentType.create(communicateImg.get().getImage().getContentType()),
                                    communicateImg.get().getImage().getContent(), communicateImg.get().getObject());
                            replyStatus.setMedia("image.jpg", new ByteArrayInputStream(communicateImg.get().getImage().getContent()));
                        }
                    }
                    replyStatus.setInReplyToStatusId(status.getId());
                    twitter.tweets().updateStatus(replyStatus);
                    ret = replyTweet;
                } else {
                    ret = null;
                }

                final SocialJournal socialJournal = new SocialJournal();
                socialJournal.setFromResponse(origLocale, avatarId,
                        realMessage, SocialChannel.TWITTER,
                        resp, Duration.millis(System.currentTimeMillis() - startTime));
                socialJournalRepo.save(socialJournal);

                return ret;
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

package org.lskk.lumen.reasoner.socmed;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.nlp.NaturalLanguage;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.lskk.lumen.reasoner.util.ImageObjectResolver;
import org.lskk.lumen.reasoner.ux.Channel;
import org.lskk.lumen.socmed.ImgurConfig;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by ceefour on 14/11/2015.
 */
public class TwitterMentionChannel extends Channel {

    private final Long inReplyToStatusId;
    private Twitter twitter;
    private ImageObjectResolver imageObjectResolver;
    private ImgurConfig imgurConfig;
    private String recipientScreenName;

    public TwitterMentionChannel(@NaturalLanguage("en") SentenceGenerator sentenceGenerator_en,
                                 @NaturalLanguage("id") SentenceGenerator sentenceGenerator_id,
                                 Twitter twitter, ImageObjectResolver imageObjectResolver,
                                 ImgurConfig imgurConfig, String recipientScreenName, Long inReplyToStatusId) {
        super(sentenceGenerator_en, sentenceGenerator_id);
        this.twitter = twitter;
        this.imageObjectResolver = imageObjectResolver;
        this.imgurConfig = imgurConfig;
        this.recipientScreenName = recipientScreenName;
        this.inReplyToStatusId = inReplyToStatusId;
    }

    @Override
    public void express(String avatarId, CommunicateAction communicateAction, Object params) {
        try {
            final boolean replyHasImage = communicateAction.getImage() != null;
            if (!communicateAction.getObject().isEmpty() || replyHasImage) {
                final int maxReplyLength = 140 - (recipientScreenName.length() + 2)
                        - (replyHasImage ? 23 : 0);
                final String replyTweet = "@" + recipientScreenName + " " + StringUtils.abbreviate(communicateAction.getObject(), maxReplyLength);
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
                if (inReplyToStatusId != null) {
                    replyStatus.setInReplyToStatusId(inReplyToStatusId);
                }
                log.info("Replying {}", replyTweet);
                twitter.tweets().updateStatus(replyStatus);
            } else {
                log.debug("Skipping empty reply to @{}", recipientScreenName);
            }
        } catch (IOException | TwitterException e) {
            throw new ReasonerException(e, "Cannot mention @%s: %s", recipientScreenName, communicateAction.getObject());
        }
    }
}

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
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.io.IOException;

/**
 * Created by ceefour on 14/11/2015.
 */
public class TwitterDirectMessageChannel extends Channel {

    private Twitter twitter;
    private ImageObjectResolver imageObjectResolver;
    private ImgurConfig imgurConfig;
    private String recipientScreenName;

    public TwitterDirectMessageChannel(@NaturalLanguage("en") SentenceGenerator sentenceGenerator_en,
                                       @NaturalLanguage("id") SentenceGenerator sentenceGenerator_id,
                                       Twitter twitter, ImageObjectResolver imageObjectResolver,
                                       ImgurConfig imgurConfig, String recipientScreenName) {
        super(sentenceGenerator_en, sentenceGenerator_id);
        this.twitter = twitter;
        this.imageObjectResolver = imageObjectResolver;
        this.imgurConfig = imgurConfig;
        this.recipientScreenName = recipientScreenName;
    }

    @Override
    public void express(CommunicateAction communicateAction) {
        // allow some characters for imgur URI
        String replyDm = StringUtils.abbreviate(communicateAction.getObject(), 10000 - 100);
        try {
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

            replyDm = replyDm.trim();
            if (!replyDm.isEmpty()) {
                log.info("DM-ing @{}: {}", recipientScreenName, replyDm);
                twitter.sendDirectMessage(recipientScreenName, replyDm);
            } else {
                log.debug("Skipping empty DM to @{}", recipientScreenName);
            }
        } catch (IOException | TwitterException e) {
            throw new ReasonerException(e, "Cannot DM @%s: %s", recipientScreenName, replyDm);
        }
    }
}

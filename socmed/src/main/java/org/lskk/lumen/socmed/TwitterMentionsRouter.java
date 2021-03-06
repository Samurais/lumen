package org.lskk.lumen.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.FluentIterable;
import org.apache.camel.Exchange;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.twitter.TwitterEndpoint;
import org.joda.time.DateTime;
import org.lskk.lumen.core.*;
import org.lskk.lumen.core.Status;
import org.lskk.lumen.core.util.AsError;
import org.lskk.lumen.core.util.ToJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import twitter4j.*;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Created by ceefour on 29/10/2015.
 */
//@Component
public class TwitterMentionsRouter extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(TwitterMentionsRouter.class);

    @Inject
    protected AgentRepository agentRepo;
    @Inject
    protected ToJson toJson;
    @Inject
    private ObjectMapper mapper;
    @Inject
    private AsError asError;

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        FluentIterable.from(agentRepo.findAll())
                .filter(it -> Optional.ofNullable(it.getTwitterSys()).map(TwitterSysConfig::getTwitterTokenSecret).isPresent())
                .forEach(ag -> {
                    final String twitterHome = UriComponentsBuilder.fromUriString("twitter://timeline/mentions?consumerKey={twitterApiKey}&consumerSecret={twitterApiSecret}&accessToken={twitterToken}&accessTokenSecret={twitterTokenSecret}")
                            .buildAndExpand(ag.getTwitterSys().getTwitterApiKey(), ag.getTwitterSys().getTwitterApiSecret(), ag.getTwitterSys().getTwitterToken(), ag.getTwitterSys().getTwitterTokenSecret()).toString();
                    from(twitterHome)
                            .to("log:twitter-mentions")
                            .process((Exchange it) -> {
                                final twitter4j.Status twitterStatus = (twitter4j.Status) it.getIn().getBody();
                                final Mention mention = new Mention();
                                mention.setThingId(String.valueOf(twitterStatus.getId()));
                                mention.setUrl("https://twitter.com/" + twitterStatus.getUser().getScreenName() + "/statuses/" + twitterStatus.getId());
                                mention.setFrom(new Person());
                                mention.getFrom().setThingId(String.valueOf(twitterStatus.getUser().getId()));
                                mention.getFrom().setSlug(twitterStatus.getUser().getScreenName());
                                mention.getFrom().setName(twitterStatus.getUser().getName());
                                mention.getFrom().setUrl("https://twitter.com/" + twitterStatus.getUser().getScreenName());
                                mention.getFrom().setPhoto(new ImageObject());
                                mention.getFrom().getPhoto().setUrl(twitterStatus.getUser().getProfileImageURLHttps());
                                mention.setMessage(twitterStatus.getText());
                                mention.setDateCreated(new DateTime(twitterStatus.getCreatedAt()));
                                mention.setDatePublished(new DateTime(twitterStatus.getCreatedAt()));
                                mention.setDateModified(new DateTime(twitterStatus.getCreatedAt()));
                                mention.setChannel(new SocialChannel());
                                mention.getChannel().setThingId("twitter");
                                mention.getChannel().setName("Twitter");
                                it.getIn().setBody(mention);
                            }).bean(toJson)
                            .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&skipQueueDeclare=true&routingKey=" + AvatarChannel.SOCIAL_PERCEPTION.key(ag.getId()))
                            .to("log:" + AvatarChannel.SOCIAL_PERCEPTION.key(ag.getId()));
                });
    }
}

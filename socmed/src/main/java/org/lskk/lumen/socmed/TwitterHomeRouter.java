package org.lskk.lumen.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.FluentIterable;
import org.apache.camel.Exchange;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.joda.time.DateTime;
import org.lskk.lumen.core.*;
import org.lskk.lumen.core.Status;
import org.lskk.lumen.core.StatusUpdate;
import org.lskk.lumen.core.util.AsError;
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
public class TwitterHomeRouter extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(TwitterHomeRouter.class);

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
                    final String twitterHome = UriComponentsBuilder.fromUriString("twitter://timeline/home?consumerKey={twitterApiKey}&consumerSecret={twitterApiSecret}&accessToken={twitterToken}&accessTokenSecret={twitterTokenSecret}")
                            .buildAndExpand(ag.getTwitterSys().getTwitterApiKey(), ag.getTwitterSys().getTwitterApiSecret(), ag.getTwitterSys().getTwitterToken(), ag.getTwitterSys().getTwitterTokenSecret()).toString();
                    from(twitterHome)
                            .to("log:twitter-home")
                            .process((Exchange it) -> {
                                final twitter4j.Status twitterStatus = (twitter4j.Status) it.getIn().getBody();
                                final StatusUpdate statusUpdate = new StatusUpdate();
                                statusUpdate.setThingId(String.valueOf(twitterStatus.getId()));
                                statusUpdate.setUrl("https://twitter.com/" + twitterStatus.getUser().getScreenName() + "/statuses/" + twitterStatus.getId());
                                statusUpdate.setFrom(new Person());
                                statusUpdate.getFrom().setThingId(String.valueOf(twitterStatus.getUser().getId()));
                                statusUpdate.getFrom().setSlug(twitterStatus.getUser().getScreenName());
                                statusUpdate.getFrom().setName(twitterStatus.getUser().getName());
                                statusUpdate.getFrom().setUrl("https://twitter.com/" + twitterStatus.getUser().getScreenName());
                                statusUpdate.getFrom().setPhoto(new ImageObject());
                                statusUpdate.getFrom().getPhoto().setUrl(twitterStatus.getUser().getProfileImageURLHttps());
                                statusUpdate.setMessage(twitterStatus.getText());
                                statusUpdate.setDateCreated(new DateTime(twitterStatus.getCreatedAt()));
                                statusUpdate.setDatePublished(new DateTime(twitterStatus.getCreatedAt()));
                                statusUpdate.setDateModified(new DateTime(twitterStatus.getCreatedAt()));
                                statusUpdate.setChannel(new SocialChannel());
                                statusUpdate.getChannel().setThingId("twitter");
                                statusUpdate.getChannel().setName("Twitter");
                                it.getIn().setBody(statusUpdate);
                            }).bean(toJson)
                            .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&skipQueueDeclare=true&routingKey=" + AvatarChannel.SOCIAL_PERCEPTION.key(ag.getId()))
                            .to("log:" + AvatarChannel.SOCIAL_PERCEPTION.key(ag.getId()));
                });
    }
}

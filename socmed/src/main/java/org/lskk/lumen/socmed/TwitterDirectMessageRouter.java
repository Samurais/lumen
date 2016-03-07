package org.lskk.lumen.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.FluentIterable;
import org.apache.camel.Exchange;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.twitter.TwitterEndpoint;
import org.joda.time.DateTime;
import org.lskk.lumen.core.*;
import org.lskk.lumen.core.util.AsError;
import org.lskk.lumen.core.util.ToJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import twitter4j.DirectMessage;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Created by ceefour on 29/10/2015.
 */
//@Component
public class TwitterDirectMessageRouter extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(TwitterDirectMessageRouter.class);

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
                    final String dmUri = UriComponentsBuilder.fromUriString("twitter://directmessage?type=polling&delay=60&consumerKey={twitterApiKey}&consumerSecret={twitterApiSecret}&accessToken={twitterToken}&accessTokenSecret={twitterTokenSecret}")
                            .buildAndExpand(ag.getTwitterSys().getTwitterApiKey(), ag.getTwitterSys().getTwitterApiSecret(), ag.getTwitterSys().getTwitterToken(), ag.getTwitterSys().getTwitterTokenSecret()).toString();
                    final TwitterEndpoint dmEndpoint = getContext().getEndpoint(dmUri, TwitterEndpoint.class);
                    from(dmEndpoint)
                            .to("log:twitter-directmessage")
                            .process((Exchange it) -> {
                                final DirectMessage directMessage = (DirectMessage) it.getIn().getBody();
                                final PrivateMessage privateMessage = new PrivateMessage();
                                privateMessage.setThingId(String.valueOf(directMessage.getId()));
//                        statusUpdate.setUrl("https://twitter.com/" + twitterStatus.getId()
                                privateMessage.setFrom(new Person());
                                privateMessage.getFrom().setThingId(String.valueOf(directMessage.getSender().getId()));
                                privateMessage.getFrom().setSlug(directMessage.getSender().getScreenName());
                                privateMessage.getFrom().setName(directMessage.getSender().getName());
                                privateMessage.getFrom().setUrl("https://twitter.com/" + directMessage.getSender().getScreenName());
                                privateMessage.getFrom().setPhoto(new ImageObject());
                                privateMessage.getFrom().getPhoto().setUrl(directMessage.getSender().getProfileImageURLHttps());
                                privateMessage.setMessage(directMessage.getText());
                                privateMessage.setDateCreated(new DateTime(directMessage.getCreatedAt()));
                                privateMessage.setDatePublished(new DateTime(directMessage.getCreatedAt()));
                                privateMessage.setDateModified(new DateTime(directMessage.getCreatedAt()));
                                privateMessage.setChannel(new SocialChannel());
                                privateMessage.getChannel().setThingId("twitter");
                                privateMessage.getChannel().setName("Twitter");
                                it.getIn().setBody(privateMessage);
                            }).bean(toJson)
                            .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&skipQueueDeclare=true&routingKey=" + AvatarChannel.SOCIAL_PERCEPTION.key(ag.getId()))
                            .to("log:" + AvatarChannel.SOCIAL_PERCEPTION.key(ag.getId()));
                });
    }
}

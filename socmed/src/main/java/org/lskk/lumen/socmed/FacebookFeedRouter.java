package org.lskk.lumen.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.FluentIterable;
import facebook4j.Post;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.facebook.FacebookEndpoint;
import org.apache.camel.component.twitter.TwitterEndpoint;
import org.joda.time.DateTime;
import org.lskk.lumen.core.*;
import org.lskk.lumen.core.util.AsError;
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
@Component
public class FacebookFeedRouter extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(FacebookFeedRouter.class);

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
        final long sometimeAgo = new DateTime().minusDays(1).getMillis() / 1000l;
        FluentIterable.from(agentRepo.findAll())
                .filter(it -> Optional.ofNullable(it.getFacebookSys()).map(FacebookSysConfig::getFacebookAppSecret).isPresent())
                .forEach(ag -> {
                    final String facebookHomeUri = UriComponentsBuilder.fromUriString("facebook://home?consumer.delay=15000&oAuthAppId={apiKey}&oAuthAppSecret={apiSecret}&oAuthAccessToken={accessToken}&reading.since={readingSince}&reading.limit=20")
                            .buildAndExpand(ag.getFacebookSys().getFacebookAppId(), ag.getFacebookSys().getFacebookAppSecret(), ag.getFacebookSys().getFacebookAccessToken(), sometimeAgo).toUriString();
                    final FacebookEndpoint facebookHome = getContext().getEndpoint(facebookHomeUri, FacebookEndpoint.class);

                    final String commentPostEndpoint = UriComponentsBuilder.fromUriString("facebook://commentPost?oAuthAppId={apiKey}&oAuthAppSecret={apiSecret}&oAuthAccessToken={accessToken}")
                            .buildAndExpand(ag.getFacebookSys().getFacebookAppId(), ag.getFacebookSys().getFacebookAppSecret(), ag.getFacebookSys().getFacebookAccessToken()).toUriString();
                    final ProducerTemplate producerTemplate = getContext().createProducerTemplate();
                    final StatusReplier replier = new StatusReplier(producerTemplate, commentPostEndpoint);
                    final EchoProcessor echoProcessor = new EchoProcessor();

                    // TODO: depends on https://issues.apache.org/jira/browse/CAMEL-8257
                    //                    final facebookHome = getContext().getEndpoint("facebook://home", FacebookEndpoint.class)
                    //                    facebookHome.configuration.setOAuthAppId(it.facebookSys.facebookAppId)
                    //                    facebookHome.configuration.setOAuthAppSecret(it.facebookSys.facebookAppSecret)
                    //                    facebookHome.configuration.setOAuthAccessToken(it.facebookSys.facebookAccessToken)

                    //                    from(facebookHome).bean(toJson).process((Exchange it) -> {
                    //                      log.debug("Headers: {}", it.getIn().headers)
                    ////                      log.debug("Body: {}", it.getIn().body)
                    //                    }.to("log:" + Channel.SOCIAL_PERCEPTION.key(it.getId()))
                    from(facebookHome).process((Exchange it) -> {
                        final Post fbPost = (Post) it.getIn().getBody();
                        final StatusUpdate statusUpdate = new StatusUpdate();
                        statusUpdate.setThingId(fbPost.getId());
                        statusUpdate.setUrl("https://www.facebook.com/" + fbPost.getId());
                        statusUpdate.setFrom(new Person());
                        statusUpdate.getFrom().setThingId(fbPost.getFrom().getId());
                        statusUpdate.getFrom().setName(fbPost.getFrom().getName());
                        statusUpdate.getFrom().setUrl("https://www.facebook.com/" + fbPost.getFrom().getId());
                        statusUpdate.getFrom().setPhoto(new ImageObject());
                        statusUpdate.getFrom().getPhoto().setUrl("https://graph.facebook.com/" + fbPost.getFrom().getId() + "/picture");
                        statusUpdate.setMessage(fbPost.getMessage() != null ? fbPost.getMessage() : fbPost.getStory());
                        statusUpdate.setDateCreated(new DateTime(fbPost.getCreatedTime()));
                        statusUpdate.setDatePublished(new DateTime(fbPost.getCreatedTime()));
                        statusUpdate.setDateModified(fbPost.getUpdatedTime() != null ? new DateTime(fbPost.getUpdatedTime()) : null);
                        statusUpdate.setChannel(new SocialChannel());
                        statusUpdate.getChannel().setThingId("facebook");
                        statusUpdate.getChannel().setName("Facebook");
                        it.getIn().setBody(statusUpdate);

                        // Echo
                        echoProcessor.processStatus(statusUpdate, replier);
                    }).bean(toJson)
                            .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + AvatarChannel.SOCIAL_PERCEPTION.key(ag.getId()))
                            .to("log:" + AvatarChannel.SOCIAL_PERCEPTION.key(ag.getId()));
                });
    }
}

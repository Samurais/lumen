package org.lskk.lumen.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.FluentIterable;
import org.apache.camel.Exchange;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.lskk.lumen.core.*;
import org.lskk.lumen.core.util.AsError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;

/**
 * Created by ceefour on 29/10/2015.
 */
@Component
public class FacebookTimelineRouter extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(FacebookTimelineRouter.class);

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
                .forEach(ag -> {
                    final String facebookFeed = UriComponentsBuilder.fromUriString("facebook://postStatusMessage?oAuthAppId={facebookAppId}&oAuthAppSecret={facebookAppSecret}&oAuthAccessToken={facebookAccessToken}")
                            .buildAndExpand(ag.getFacebookSys().getFacebookAppId(), ag.getFacebookSys().getFacebookAppSecret(), ag.getFacebookSys().getFacebookAccessToken()).toString();
                    final String twitterTimelineUser = UriComponentsBuilder.fromUriString("twitter://timeline/user?consumerKey={twitterApiKey}&consumerSecret={twitterApiSecret}&accessToken={twitterToken}&accessTokenSecret={twitterTokenSecret}")
                            .buildAndExpand(ag.getTwitterSys().getTwitterApiKey(), ag.getTwitterSys().getTwitterApiSecret(), ag.getTwitterSys().getTwitterToken(), ag.getTwitterSys().getTwitterTokenSecret()).toString();
                    from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + LumenChannel.FACEBOOK_TIMELINE_OUT.key(ag.getId()))
                            .to("log:" + LumenChannel.FACEBOOK_TIMELINE_OUT.key(ag.getId()))
                            .process((Exchange it) -> {
                                final CommunicateAction communicateAction = toJson.mapper.readValue((byte[]) it.getIn().getBody(), CommunicateAction.class);
                                it.getIn().setHeader("network.id", "facebook");
                                it.getIn().setHeader("CamelFacebook.message", communicateAction.getObject());
                                it.getIn().setBody(null);
//                                final StatusUpdate statusUpdate = toJson.mapper.readValue((byte[]) it.getIn().getBody(), StatusUpdate.class);
//                                switch (statusUpdate.getChannel().getThingId()) {
//                                    case "facebook":
//                                        it.getIn().setHeader("network.id", "facebook");
//                                        it.getIn().setHeader("CamelFacebook.message", statusUpdate.getMessage());
//                                        it.getIn().setBody(null);
//                                        break;
//                                    case "twitter":
//                                        it.getIn().setHeader("network.id", "twitter");
//                                        it.getIn().setBody(statusUpdate.getMessage());
//                                        break;
//                                }
                            }).choice()
                            .when(header("network.id").isEqualTo("facebook")).to(facebookFeed).to("log:" + LumenChannel.FACEBOOK_TIMELINE_OUT.key(ag.getId()) + "-postStatusMessage")
                            .when(header("network.id").isEqualTo("twitter")).to(twitterTimelineUser).to("log:twitter-timeline-user")
                            .otherwise().to("log:socmed-unknown");
                });
    }
}

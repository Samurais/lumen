package org.lskk.lumen.reasoner;

import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.lskk.lumen.core.*;
import org.lskk.lumen.core.util.AsError;
import org.lskk.lumen.reasoner.aiml.AimlService;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.lskk.lumen.reasoner.expression.Proposition;
import org.lskk.lumen.reasoner.social.SocialJournal;
import org.lskk.lumen.reasoner.social.SocialJournalRepository;
import org.lskk.lumen.reasoner.ux.ChatChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 10/2/15.
 */
@Component
@Profile("reasonerApp")
public class ReasonerRouter extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(ReasonerRouter.class);

    @Inject
    private ToJson toJson;
    @Inject
    private AsError asError;
    @Inject
    private AimlService aimlService;
    @Inject
    private ChatChannel chatChannel;
    @Inject
    private DroolsService droolsService;
    @Inject
    private SocialJournalRepository socialJournalRepo;

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
//        from("timer:hello?period=3s")
//                .process(exchange -> {
//                    exchange.getIn().setBody(new GreetingReceived("Hendy"));
//                })
//                .to("seda:greetingReceived");

//        from("timer:tell me a good story?period=1s&repeatCount=1")
//                .process(exchange -> {
//                    final AgentResponse agentResponse = aimlService.process(Locale.US, "tell me a good story", logChannel);
//                    droolsService.process(agentResponse);
//                });

        final String agentId = "arkan";
        from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&queue=" + AvatarChannel.CHAT_INBOX.wildcard() + "&routingKey=" + AvatarChannel.CHAT_INBOX.wildcard())
                .process(exchange -> {
                    final long startTime = System.currentTimeMillis();
                    final CommunicateAction inCommunicate = toJson.getMapper().readValue(
                            exchange.getIn().getBody(byte[].class), CommunicateAction.class);
                    inCommunicate.setAvatarId(AvatarChannel.getAvatarId((String) exchange.getIn().getHeader(RabbitMQConstants.ROUTING_KEY)));
                    log.info("Chat inbox for {}: {}", inCommunicate.getAvatarId(), inCommunicate);

                    final Locale origLocale = Optional.ofNullable(inCommunicate.getInLanguage()).orElse(Locale.US);
                    final AgentResponse agentResponse = aimlService.process(origLocale, inCommunicate.getObject(),
                            chatChannel, inCommunicate.getAvatarId());

                    final SocialJournal socialJournal = new SocialJournal();
                    socialJournal.setAvatarId(inCommunicate.getAvatarId());
                    socialJournal.setAgentId(agentId);
                    socialJournal.setSocialChannelId(SocialChannel.DIRECT.getThingId());
                    socialJournal.setReceivedLanguage(Optional.ofNullable(agentResponse.getStimuliLanguage()).orElse(origLocale));
                    socialJournal.setReceivedText(inCommunicate.getObject());
                    socialJournal.setResponseInsertables(
                            agentResponse.getInsertables().stream()
                                    .map(it -> it.getClass().getName()).collect(Collectors.joining(", ")));
                    socialJournal.setTruthValue(new SimpleTruthValue(agentResponse.getMatchingTruthValue()));

                    if (agentResponse.getCommunicateAction() != null) {
                        chatChannel.express(inCommunicate.getAvatarId(), agentResponse.getCommunicateAction(), null);
                        socialJournal.setResponseKind(agentResponse.getCommunicateAction().getClass().getName());
                        socialJournal.setResponseLanguage(agentResponse.getCommunicateAction().getInLanguage());
                        socialJournal.setResponseText(agentResponse.getCommunicateAction().getObject());
                    } else if (agentResponse.getUnrecognizedInput() != null) {
                        chatChannel.express(inCommunicate.getAvatarId(), Proposition.I_DONT_UNDERSTAND, null);
                        socialJournal.setResponseKind(agentResponse.getUnrecognizedInput().getClass().getName());
                    }
                    droolsService.process(agentResponse);
                    socialJournal.setProcessingTime((System.currentTimeMillis() - startTime) / 1000f);

                    socialJournalRepo.save(socialJournal);

                    exchange.getIn().setBody(new Status());
                })
                .bean(toJson);
    }

}

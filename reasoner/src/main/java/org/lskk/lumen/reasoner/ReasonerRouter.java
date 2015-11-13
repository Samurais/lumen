package org.lskk.lumen.reasoner;

import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.lskk.lumen.core.util.AsError;
import org.lskk.lumen.reasoner.aiml.AimlService;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.lskk.lumen.reasoner.event.GreetingReceived;
import org.lskk.lumen.reasoner.ux.LogChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Locale;

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
    private LogChannel logChannel;
    @Inject
    private DroolsService droolsService;

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
//        from("timer:hello?period=3s")
//                .process(exchange -> {
//                    exchange.getIn().setBody(new GreetingReceived("Hendy"));
//                })
//                .to("seda:greetingReceived");

        from("timer:tell me a good story?period=1s&repeatCount=1")
                .process(exchange -> {
                    final AgentResponse agentResponse = aimlService.process(Locale.US, "tell me a good story", logChannel);
                    droolsService.process(agentResponse);
                });
    }

}

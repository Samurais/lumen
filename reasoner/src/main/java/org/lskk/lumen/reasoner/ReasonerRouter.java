package org.lskk.lumen.reasoner;

import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.lskk.lumen.core.util.AsError;
import org.lskk.lumen.reasoner.event.GreetingReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

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

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        from("timer:hello?period=3s")
                .process(exchange -> {
                    exchange.getIn().setBody(new GreetingReceived("Hendy"));
                })
                .to("seda:greetingReceived");
    }
}

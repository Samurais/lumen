package org.lskk.lumen.reasoner;

import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.kie.api.runtime.KieSession;
import org.lskk.lumen.core.util.AsError;
import org.lskk.lumen.core.util.ToJson;
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
public class DroolsRouter extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(DroolsRouter.class);

    @Inject
    private ToJson toJson;
    @Inject
    private AsError asError;
    @Inject
    private KieSession kieSession;

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
        from("seda:greetingReceived")
                .process(exchange -> {
                    final GreetingReceived greetingReceived = exchange.getIn().getBody(GreetingReceived.class);
                    kieSession.insert(greetingReceived);
                    kieSession.fireAllRules();
                })
                .to("log:echo");
    }
}

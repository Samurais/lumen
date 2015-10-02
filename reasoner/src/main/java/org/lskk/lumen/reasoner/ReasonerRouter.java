package org.lskk.lumen.reasoner;

import org.apache.camel.builder.RouteBuilder;
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

    @Override
    public void configure() throws Exception {
        from("timer:hello?period=3s").to("log:hello");
    }
}

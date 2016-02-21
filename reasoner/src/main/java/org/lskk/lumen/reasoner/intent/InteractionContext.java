package org.lskk.lumen.reasoner.intent;

import org.lskk.lumen.reasoner.ux.Fragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inspired by Camel's {@link org.apache.camel.Exchange} + {@link org.apache.camel.ProducerTemplate} + Wicket's {@link org.apache.wicket.request.cycle.RequestCycle}.
 * Created by ceefour on 18/02/2016.
 */
public class InteractionContext {
    private static final Logger log = LoggerFactory.getLogger(InteractionContext.class);

    public void reply(Fragment fragment) {
        log.info("Replying with {}", fragment);
    }

}
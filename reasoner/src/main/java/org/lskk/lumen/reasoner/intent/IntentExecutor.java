package org.lskk.lumen.reasoner.intent;

import com.google.common.collect.ImmutableMap;
import org.lskk.lumen.persistence.service.FactService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.net.URL;

/**
 * Created by ceefour on 17/02/2016.
 */
@Service
public class IntentExecutor {

    @Inject
    private FactService factService;

    /**
     * Executes activity of an {@link Intent}.
     * Typically behavior is written in JavaScript and executed using Nashorn.
     * However it's possible for more complex behaviors to be written
     * using either MVEL/DRL or Java class implementation.
     * JavaScript scripting is done in the same style as Unity's.
     * @param intent
     * @param interactionContext
     */
    public void executeIntent(Intent intent, InteractionContext interactionContext) {
        final String scriptUrl = "/intents/" + intent.getIntentTypeId() + ".js";
        final URL scriptRes = IntentExecutor.class.getResource(scriptUrl);
        final JavaScriptIntentBehavior behavior = new JavaScriptIntentBehavior(scriptRes);
        final ImmutableMap<String, FactService> services = ImmutableMap.of("factService", factService);
        behavior.start(intent, interactionContext, services);
    }

}

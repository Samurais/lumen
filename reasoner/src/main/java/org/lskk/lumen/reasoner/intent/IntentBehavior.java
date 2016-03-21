package org.lskk.lumen.reasoner.intent;

import org.lskk.lumen.persistence.service.FactService;

import java.util.Map;

/**
 * Created by ceefour on 17/02/2016.
 */
public abstract class IntentBehavior {

    public abstract void start(Intent intent,
                               InteractionContext interactionContext, Map<String, FactService> services);

}

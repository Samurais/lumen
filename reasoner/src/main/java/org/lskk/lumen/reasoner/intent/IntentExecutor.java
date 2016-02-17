package org.lskk.lumen.reasoner.intent;

import org.springframework.stereotype.Service;

/**
 * Created by ceefour on 17/02/2016.
 */
@Service
public class IntentExecutor {

    /**
     * Executes activity of an {@link Intent}.
     * Typically behavior is written in JavaScript and executed using Nashorn.
     * However it's possible for more complex behaviors to be written
     * using either MVEL/DRL or Java class implementation.
     * JavaScript scripting is done in the same style as Unity's.
     * @param intent
     * @param interactionContext
     */
    public void executeIntent(Intent intent, Object interactionContext) {


    }

}

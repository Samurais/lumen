package org.lskk.lumen.reasoner.intent;

import jdk.nashorn.api.scripting.URLReader;
import org.lskk.lumen.reasoner.ReasonerException;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Created by ceefour on 17/02/2016.
 */
public class JavaScriptIntentBehavior extends IntentBehavior {

    private final ScriptEngine engine;
    private final Invocable invocable;

    public JavaScriptIntentBehavior(URL scriptUrl) {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            engine.eval(new URLReader(scriptUrl, StandardCharsets.UTF_8));
            invocable = (Invocable) engine;
        } catch (ScriptException e) {
            throw new ReasonerException(e, "Cannot load behavior %s", scriptUrl);
        }
    }

    @Override
    public void start(Intent intent, Object interactionContext) {
        try {
            invocable.invokeFunction("start", intent, interactionContext);
        } catch (Exception e) {
            throw new ReasonerException(e, "Error running start intent %s", intent);
        }
    }

}

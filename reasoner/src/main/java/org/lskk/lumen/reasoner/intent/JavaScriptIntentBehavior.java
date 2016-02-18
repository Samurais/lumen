package org.lskk.lumen.reasoner.intent;

import com.google.common.collect.ImmutableList;
import jdk.nashorn.api.scripting.URLReader;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.ReasonerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 17/02/2016.
 */
public class JavaScriptIntentBehavior extends IntentBehavior {

    private final ScriptEngine engine;
    private final Invocable invocable;

    public static class JavaScriptLogging {

        private final Logger log;

        public JavaScriptLogging(String intentTypeId) {
            log = LoggerFactory.getLogger(JavaScriptLogging.class.getName() + "." + intentTypeId);
        }

        public void info(Object... args) {
            log.info(ImmutableList.copyOf(args).stream().map(Objects::toString).collect(Collectors.joining(" ")));
        }
    }

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
    public void start(Intent intent, InteractionContext interactionContext, Map<String, FactService> services) {
        try {
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("log", new JavaScriptLogging(intent.getIntentTypeId()));
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("intent", intent);
            invocable.invokeFunction("start", intent, interactionContext);
        } catch (Exception e) {
            throw new ReasonerException(e, "Error running start intent %s", intent);
        }
    }

}

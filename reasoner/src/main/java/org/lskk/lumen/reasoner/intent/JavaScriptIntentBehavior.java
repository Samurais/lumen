package org.lskk.lumen.reasoner.intent;

import jdk.nashorn.api.scripting.URLReader;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.ux.Fragment;
import org.lskk.lumen.reasoner.ux.NuiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * We try not to provide any syntax sugar, to ensure maximum portability and similar APIs between JavaScript, MVEL/DRL,
 * ClojureScript, and Scala intent behaviors.
 * Created by ceefour on 17/02/2016.
 * @see org.lskk.lumen.reasoner.activity.Script
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
            log.info(Arrays.asList(args).stream().map(Objects::toString).collect(Collectors.joining(" ")));
        }
    }

    public JavaScriptIntentBehavior(URL scriptUrl) {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
//            final String origScript = IOUtils.toString(scriptUrl, StandardCharsets.UTF_8);
//            final String augmentedScript = "with(new JavaImporter(org.lskk.lumen.reasoner.ux)) {\n"
//                    + origScript + "\n}\n";
            engine.eval(new URLReader(scriptUrl, StandardCharsets.UTF_8));
            invocable = (Invocable) engine;
        } catch (Exception e) {
            throw new ReasonerException(e, "Cannot load behavior %s", scriptUrl);
        }
    }

    @Override
    public synchronized void start(Intent intent, InteractionContext interactionContext, Map<String, FactService> services) {
        try {
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("log", new JavaScriptLogging(intent.getIntentTypeId()));
//            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("intent", intent);
            final HashMap<String, Object> intentMap = new HashMap<>();
            BeanUtils.copyProperties(intent, intentMap);
            intentMap.putAll(intent.getParameters());
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("intent", intentMap);
            engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(services);
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("interaction", interactionContext);
            invocable.invokeFunction("start", intent, interactionContext);
        } catch (Exception e) {
            throw new ReasonerException(e, "Error running start intent %s", intent);
        }
    }

}

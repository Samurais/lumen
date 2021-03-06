package org.lskk.lumen.reasoner.activity;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import jdk.nashorn.api.scripting.URLReader;
import org.lskk.lumen.reasoner.intent.Slot;

import javax.annotation.Nullable;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by ceefour on 04/03/2016.
 * @see org.lskk.lumen.reasoner.intent.JavaScriptIntentBehavior
 */
public class Script extends Activity {

    /**
     *
     * @param previous
     * @param current
     * @param locale Specific {@link Locale} that was active during the state change, it's always one of {@link InteractionSession#getActiveLocales()}.
     * @param session Only {@link InteractionSession#getScriptables()} is used.
     * @throws Exception
     */
    @Override
    public void onStateChanged(ActivityState previous, ActivityState current, Locale locale, @Nullable InteractionSession session) throws Exception {
        super.onStateChanged(previous, current, locale, session);
        if (ActivityState.ACTIVE == current) {
            final String scriptPath = "/org/lskk/lumen/reasoner/activity/" + getId() + ".js";
            final URL scriptUrl = Preconditions.checkNotNull(Script.class.getResource(scriptPath),
                    "Cannot find script '%s' file in classpath: %s", getId(), scriptPath);
            final ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            engine.eval(new URLReader(scriptUrl, StandardCharsets.UTF_8));
            final Invocable invocable = (Invocable) engine;

            final Map<String, Object> realScriptables = Optional.ofNullable(session.getScriptables()).orElse(ImmutableMap.of());
            log.debug("{} '{}' provides {} scriptables: {}", getClass().getSimpleName(), getPath(),
                    realScriptables.size(), realScriptables.keySet());
            engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(realScriptables);

            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("log", log);
//            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("intent", intent);

            final LinkedHashMap<String, Slot> inSlotsVar = new LinkedHashMap<>();
            getInSlots().forEach(slot -> inSlotsVar.put(slot.getId(), slot));
            final LinkedHashMap<String, Slot> outSlotsVar = new LinkedHashMap<>();
            getOutSlots().forEach(slot -> outSlotsVar.put(slot.getId(), slot));
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("inSlots", inSlotsVar);
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("outSlots", outSlotsVar);
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("pendingCommunicateActions", getPendingCommunicateActions());
            invocable.invokeFunction("onActivate");

            // if everything's good, the Script can complete
            session.complete(this, locale);
        }
    }
}

package org.lskk.lumen.reasoner.activity;

import jdk.nashorn.api.scripting.URLReader;
import org.apache.commons.beanutils.BeanUtils;
import org.lskk.lumen.reasoner.intent.Slot;

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

    private List<Slot> outSlots = new ArrayList<>();

    public List<Slot> getOutSlots() {
        return outSlots;
    }

    @Override
    public void initialize() {
        super.initialize();
        outSlots.forEach(it -> it.initialize(Slot.Direction.OUT));
    }

    @Override
    public void onStateChanged(ActivityState previous, ActivityState current, Locale locale, InteractionSession session) throws Exception {
        super.onStateChanged(previous, current, locale, session);
        if (ActivityState.ACTIVE == current) {
            final URL scriptUrl = Script.class.getResource("/org/lskk/lumen/reasoner/activity/" + getId() + ".js");
            final ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            engine.eval(new URLReader(scriptUrl, StandardCharsets.UTF_8));
            final Invocable invocable = (Invocable) engine;

            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("log", log);
//            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("intent", intent);

            final LinkedHashMap<String, Slot> inSlotsVar = new LinkedHashMap<>();
            getInSlots().forEach(slot -> inSlotsVar.put(slot.getId(), slot));
            final LinkedHashMap<String, Slot> outSlotsVar = new LinkedHashMap<>();
            getOutSlots().forEach(slot -> outSlotsVar.put(slot.getId(), slot));
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("inSlots", inSlotsVar);
            engine.getBindings(ScriptContext.ENGINE_SCOPE).put("outSlots", outSlotsVar);
            invocable.invokeFunction("onActivate");
        }
    }
}

package org.lskk.lumen.reasoner.activity;

import javax.inject.Qualifier;
import java.lang.annotation.*;

/**
 * Marks the {@link org.springframework.stereotype.Service} usable by {@link Script},
 * and will be exposed as {@link javax.script.ScriptContext#ENGINE_SCOPE} binding.
 * Created by ceefour on 06/03/2016.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Scriptable {
}

package org.lskk.lumen.persistence.neo4j;

import java.lang.annotation.*;

/**
 * Created by ceefour on 22/02/2016.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnsureList {
    Ensure[] value();
}

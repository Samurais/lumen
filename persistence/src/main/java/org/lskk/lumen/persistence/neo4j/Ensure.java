package org.lskk.lumen.persistence.neo4j;

import java.lang.annotation.*;

/**
 * Created by ceefour on 22/02/2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
@Repeatable(EnsureList.class)
public @interface Ensure {
    String value();
}

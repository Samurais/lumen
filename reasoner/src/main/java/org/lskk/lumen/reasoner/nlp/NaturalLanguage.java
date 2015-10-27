package org.lskk.lumen.reasoner.nlp;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by ceefour on 27/10/2015.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface NaturalLanguage {
    String value();
}

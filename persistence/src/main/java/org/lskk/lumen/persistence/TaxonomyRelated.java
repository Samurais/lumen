package org.lskk.lumen.persistence;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by ceefour on 11/02/2016.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TaxonomyRelated {
}

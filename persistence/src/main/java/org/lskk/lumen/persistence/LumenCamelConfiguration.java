package org.lskk.lumen.persistence;

import groovy.transform.CompileStatic;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Configuration
@Profile("daemon")
public class LumenCamelConfiguration extends CamelConfiguration {
}

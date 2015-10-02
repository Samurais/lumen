package org.lskk.lumen.reasoner;

import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.inject.Inject;

/**
 * Created by ceefour on 10/2/15.
 */
@Configuration
public class DroolsConfig {

    private static final Logger log = LoggerFactory.getLogger(DroolsConfig.class);

    @Inject
    private Environment env;

    @Bean
    public KieServices kieServices() {
        return KieServices.Factory.get();
    }

    @Bean
    public KieContainer kieContainer() {
        return kieServices().getKieClasspathContainer(DroolsConfig.class.getClassLoader());
    }

    @Bean(destroyMethod = "dispose")
    public KieSession kieSession() {
        return kieContainer().newKieSession();
    }

    // https://issues.jboss.org/browse/DROOLS-937
    /*@Bean(destroyMethod = "shutdown")
    public KieScanner kieScanner() {
        final KieScanner kieScanner = kieServices().newKieScanner(kieContainer());
        kieScanner.start(10000l); // 10 seconds
        return kieScanner;
    }*/
}

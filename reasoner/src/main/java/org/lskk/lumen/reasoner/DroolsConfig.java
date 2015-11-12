package org.lskk.lumen.reasoner;

import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

/**
 * Created by ceefour on 10/2/15.
 */
@Configuration
@Profile("reasonerApp")
public class DroolsConfig {

    private static final Logger log = LoggerFactory.getLogger(DroolsConfig.class);

    @Inject
    private Environment env;
    private KieContainer kieContainer;
    private KieSession kieSession;

    @Bean
    public KieServices kieServices() {
        final KieServices kieServices = KieServices.Factory.get();
        return kieServices;
    }

    public void restart() {
        stop();
        start();
    }

    @PostConstruct
    public void start() {
        // FIXME: exclude *.csv files from being treated as decision table
        kieSession = kieContainer().newKieSession();
        log.info("Starting {}", kieSession);
        kieSession.setGlobal("log", log);
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping {}", kieSession);
        kieSession.dispose();
        kieSession = null;
    }

    @Bean
    public KieContainer kieContainer() {
        kieContainer = kieServices().getKieClasspathContainer(DroolsConfig.class.getClassLoader());
        return kieContainer;
    }

    @Bean @Scope("prototype")
    public KieSession kieSession() {
        return kieSession;
    }

    // https://issues.jboss.org/browse/DROOLS-937
    /*@Bean(destroyMethod = "shutdown")
    public KieScanner kieScanner() {
        final KieScanner kieScanner = kieServices().newKieScanner(kieContainer());
        kieScanner.start(10000l); // 10 seconds
        return kieScanner;
    }*/
}

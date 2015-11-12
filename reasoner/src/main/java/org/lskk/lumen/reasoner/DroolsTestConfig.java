package org.lskk.lumen.reasoner;

import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.lskk.lumen.reasoner.story.StoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.inject.Inject;

/**
 * Created by ceefour on 10/2/15.
 */
@Configuration
@Profile("test")
public class DroolsTestConfig {

    private static final Logger log = LoggerFactory.getLogger(DroolsTestConfig.class);

    @Inject
    private Environment env;
    @Inject
    private StoryRepository storyRepo;

    @Bean
    public KieServices kieServices() {
        final KieServices kieServices = KieServices.Factory.get();
        return kieServices;
    }

    @Bean
    public KieContainer kieContainer() {
        final KieContainer kieContainer = kieServices().getKieClasspathContainer(DroolsTestConfig.class.getClassLoader());
        return kieContainer;
    }

    @Bean
    public KieBase kieBase() {
        final KieBaseConfiguration kieBaseConfig = kieServices().newKieBaseConfiguration();
        kieBaseConfig.setOption(EventProcessingOption.STREAM);
        final KieBase kieBase = kieContainer().newKieBase(kieBaseConfig);
        return kieBase;
    }

    @Bean(destroyMethod = "dispose")
    public KieSession kieSession() {
        final KieSessionConfiguration config = kieServices().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo"));
        // FIXME: exclude *.csv files from being treated as decision table
        final KieSession kieSession = kieBase().newKieSession(config, null);
        kieSession.setGlobal("log", log);
        kieSession.setGlobal("storyRepo", storyRepo);
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

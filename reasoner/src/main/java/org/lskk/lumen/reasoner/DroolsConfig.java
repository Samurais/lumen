package org.lskk.lumen.reasoner;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.drools.core.audit.WorkingMemoryConsoleLogger;
import org.drools.core.event.DebugAgendaEventListener;
import org.drools.core.event.DebugRuleRuntimeEventListener;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.internal.builder.conf.RuleEngineOption;
import org.lskk.lumen.reasoner.quran.LiteralRepository;
import org.lskk.lumen.reasoner.quran.QuranChapterRepository;
import org.lskk.lumen.reasoner.quran.QuranService;
import org.lskk.lumen.reasoner.quran.QuranVerseRepository;
import org.lskk.lumen.reasoner.story.StoryRepository;
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
import java.util.concurrent.Executors;

/**
 * Created by ceefour on 10/2/15.
 */
@Configuration
@Profile("drools")
public class DroolsConfig {

    private static final Logger log = LoggerFactory.getLogger(DroolsConfig.class);

    @Inject
    private Environment env;
    @Inject
    private StoryRepository storyRepo;
    @Inject
    private QuranService quranSvc;
    @Inject
    private LiteralRepository literalRepo;
    @Inject
    private QuranChapterRepository quranChapterRepo;
    @Inject
    private QuranVerseRepository quranVerseRepo;


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

    @Bean(destroyMethod = "shutdown")
    public ListeningExecutorService executor() {
        return MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    }

    @PostConstruct
    public void start() {
        // FIXME: exclude *.csv files from being treated as decision table
        final KieSessionConfiguration config = kieServices().newKieSessionConfiguration();
        config.setOption(ClockTypeOption.get("realtime"));
        // FIXME: exclude *.csv files from being treated as decision table
        kieSession = kieBase().newKieSession(config, null);
        kieSession.setGlobal("log", log);
        kieSession.setGlobal("env", env);
        kieSession.setGlobal("storyRepo", storyRepo);
        kieSession.setGlobal("quranSvc", quranSvc);
        kieSession.setGlobal("literalRepo", literalRepo);
        kieSession.setGlobal("quranChapterRepo", quranChapterRepo);
        kieSession.setGlobal("quranVerseRepo", quranVerseRepo);
//        kieSession.addEventListener(new DebugAgendaEventListener());
//        kieSession.addEventListener(new DebugRuleRuntimeEventListener());
        executor().submit(() -> {
            log.info("Starting {}", kieSession);
            kieSession.fireUntilHalt();
            log.info("{} halted", kieSession);
        });
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping {}", kieSession);
        kieSession.halt();
        kieSession.dispose();
        kieSession = null;
    }

    @Bean
    public KieContainer kieContainer() {
        kieContainer = kieServices().getKieClasspathContainer(DroolsConfig.class.getClassLoader());
        return kieContainer;
    }

    @Bean
    public KieBase kieBase() {
        final KieBaseConfiguration kieBaseConfig = kieServices().newKieBaseConfiguration();
        kieBaseConfig.setOption(EventProcessingOption.STREAM);
        final KieBase kieBase = kieContainer().newKieBase(kieBaseConfig);
        return kieBase;
    }

    @Bean(destroyMethod = "dispose") @Scope("prototype")
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

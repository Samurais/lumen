package org.lskk.lumen.reasoner.interaction;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.LumenCoreConfig;
import org.lskk.lumen.core.LumenLocale;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.nlp.WordNetConfig;
import org.lskk.lumen.reasoner.ux.Channel;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Locale;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * uses Mockito: http://www.baeldung.com/injecting-mocks-in-spring
 * Created by ceefour on 18/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = JavaScriptIntentTest.IntentConfig.class)
@SpringApplicationConfiguration(InteractionSessionTest.Config.class)
@ActiveProfiles("InteractionSessionTest")
public class InteractionSessionTest {

    @Profile("InteractionSessionTest")
    @SpringBootApplication(scanBasePackageClasses = {PromptTask.class, WordNetConfig.class},
            exclude = {HibernateJpaAutoConfiguration.class, DataSourceAutoConfiguration.class, JmxAutoConfiguration.class, CamelAutoConfiguration.class, GroovyTemplateAutoConfiguration.class})
    @Import({LumenCoreConfig.class})
//    @Configuration
//    @ComponentScan(basePackageClasses = {IntentExecutor.class, ThingRepository.class, YagoTypeRepository.class, FactServiceImpl.class})
//    @EnableJpaRepositories(basePackageClasses = YagoTypeRepository.class)
//    @EnableNeo4jRepositories(basePackageClasses = ThingRepository.class)
    public static class Config {
        @Bean
        public Channel<Void> mockChannel() {
//            return new LogChannel();
            return mock(Channel.class, withSettings().verboseLogging());
        }

        @Bean
        public FactService factService() {
            return mock(FactService.class, withSettings()/*.verboseLogging()*/);
        }
    }

    @Inject
    private InteractionTaskRepository promptTaskRepo;
    @Inject
    private Channel<Void> mockChannel;
    @Inject
    private FactService factService;
    @Inject
    private Provider<InteractionSession> sessionProvider;

    @Test
    public void askNameThenAssert() {
        reset(factService, mockChannel);
        try (final InteractionSession session = sessionProvider.get()) {
            session.getActiveLocales().add(LumenLocale.INDONESIAN);
            session.getActiveLocales().add(Locale.US);
            session.open();
            final PromptTask promptName = promptTaskRepo.createPrompt("promptName");
            final AffirmationTask affirmationTask = new AffirmationTask();
            promptName.setAffirmationTask(affirmationTask);
            session.getTasks().add(promptName);
            session.getTasks().add(affirmationTask);

            session.activate(promptName, LumenLocale.INDONESIAN);
            session.update(mockChannel);

            session.receiveUtterance(LumenLocale.INDONESIAN, "namaku Hendy Irawan", factService);
            verify(factService, times(5)).assertLabel(any(), any(), any(), eq("id-ID"), any(), any(), any());
            verify(factService, times(0)).assertPropertyToLiteral(any(), any(), any(), any(), any(), any(), any());

            session.update(mockChannel);
            verify(mockChannel, times(2)).express(any(), any(), any());
        }
    }

}

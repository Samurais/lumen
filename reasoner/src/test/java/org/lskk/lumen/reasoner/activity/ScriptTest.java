package org.lskk.lumen.reasoner.activity;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.LumenCoreConfig;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.intent.Slot;
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
import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Created by ceefour on 18/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(ScriptTest.Config.class)
@ActiveProfiles("ScriptTest")
public class ScriptTest {

    @Profile("ScriptTest")
    @SpringBootApplication(scanBasePackageClasses = {ScriptRepository.class/*, WordNetConfig.class*/},
            exclude = {HibernateJpaAutoConfiguration.class, DataSourceAutoConfiguration.class, JmxAutoConfiguration.class, CamelAutoConfiguration.class, GroovyTemplateAutoConfiguration.class})
    @Import({LumenCoreConfig.class})
//    @Configuration
//    @ComponentScan(basePackageClasses = {IntentExecutor.class, ThingRepository.class, YagoTypeRepository.class, FactServiceImpl.class})
//    @EnableJpaRepositories(basePackageClasses = YagoTypeRepository.class)
//    @EnableNeo4jRepositories(basePackageClasses = ThingRepository.class)
    public static class Config {
//        @Bean
//        public Channel<Void> mockChannel() {
////            return new LogChannel();
//            return mock(Channel.class, withSettings().verboseLogging());
//        }

        @Bean
        public FactService mockFactService() {
            return mock(FactService.class, withSettings()/*.verboseLogging()*/);
        }
    }

    @Inject
    private ScriptRepository scriptRepo;
    //    @Inject
//    private TaskRepository taskRepo;
//    @Inject
//    private Channel<Void> mockChannel;
    @Inject
    private FactService factService;
//    @Inject
//    private Provider<InteractionSession> sessionProvider;

    @Test
    public void scriptsLoaded() {
        assertThat(scriptRepo.getScripts().values(), hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    public void convertUnitScript() throws Exception {
        final Script convertUnitScript = scriptRepo.get("convertUnit");
        assertThat(convertUnitScript.getId(), equalTo("convertUnit"));
        assertThat(convertUnitScript.getInSlots().stream().map(Slot::getId).toArray(), Matchers.arrayContaining("measure", "unit"));
        assertThat(convertUnitScript.getOutSlots().stream().map(Slot::getId).toArray(), Matchers.arrayContaining("converted"));
        convertUnitScript.onStateChanged(ActivityState.PENDING, ActivityState.ACTIVE, Locale.forLanguageTag("id-ID"), null);
    }

}

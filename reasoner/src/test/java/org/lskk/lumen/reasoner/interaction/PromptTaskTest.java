package org.lskk.lumen.reasoner.interaction;

import com.google.common.collect.ImmutableMap;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.LumenCoreConfig;
import org.lskk.lumen.core.LumenLocale;
import org.lskk.lumen.core.RabbitMqConfig;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.service.MatchingThing;
import org.lskk.lumen.reasoner.intent.Intent;
import org.lskk.lumen.reasoner.intent.InteractionContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by ceefour on 18/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = JavaScriptIntentTest.IntentConfig.class)
@SpringApplicationConfiguration(PromptTaskTest.Config.class)
public class PromptTaskTest {

    @SpringBootApplication(scanBasePackageClasses = {PromptTask.class},
        exclude = {HibernateJpaAutoConfiguration.class, DataSourceAutoConfiguration.class, JmxAutoConfiguration.class, CamelAutoConfiguration.class, GroovyTemplateAutoConfiguration.class})
    @Import({LumenCoreConfig.class})
//    @Configuration
//    @ComponentScan(basePackageClasses = {IntentExecutor.class, ThingRepository.class, YagoTypeRepository.class, FactServiceImpl.class})
//    @EnableJpaRepositories(basePackageClasses = YagoTypeRepository.class)
//    @EnableNeo4jRepositories(basePackageClasses = ThingRepository.class)
    public static class Config {

    }

    @Inject
    private PromptTaskRepository promptTaskRepo;

    @Test
    public void promptBirthDate() {
        final PromptTask promptBirthdate = promptTaskRepo.get("promptBirthdate");
        assertThat(promptBirthdate.getAskSsmls(), hasSize(greaterThan(1)));
        assertThat(promptBirthdate.getUtterancePatterns(), hasSize(greaterThan(1)));
        assertThat(promptBirthdate.getProperty(), equalTo("yago:wasBornOnDate"));
        assertThat(promptBirthdate.getExpectedTypes(), contains("xs:date"));

        assertThat(promptBirthdate.getPrompt(LumenLocale.INDONESIAN).getObject(), containsString("lahir"));
        assertThat(promptBirthdate.getPrompt(Locale.US).getObject(), containsString("born"));

        final List<UtterancePattern> matches = promptBirthdate.matchUtterance(LumenLocale.INDONESIAN, "Aku lahir tanggal 14 Desember 1983.",
                UtterancePattern.Scope.ANY);
        assertThat(matches, hasSize(2));
        assertThat(matches.get(0).getSlotStrings(), equalTo(ImmutableMap.of("birthdate", "14 Desember 1983")));
        assertThat(matches.get(1).getSlotStrings(), equalTo(ImmutableMap.of("birthdate", "14 Desember 1983")));
        assertThat(matches.get(0).getSlotValues(), equalTo(ImmutableMap.of("birthdate", new LocalDate("1983-12-14"))));
        assertThat(matches.get(1).getSlotValues(), equalTo(ImmutableMap.of("birthdate", new LocalDate("1983-12-14"))));
    }

}

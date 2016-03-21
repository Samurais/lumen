package org.lskk.lumen.reasoner.activity;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.LumenCoreConfig;
import org.lskk.lumen.core.LumenLocale;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.nlp.WordNetConfig;
import org.lskk.lumen.reasoner.skill.Skill;
import org.lskk.lumen.reasoner.skill.SkillRepository;
import org.lskk.lumen.reasoner.ux.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by ceefour on 18/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(UnitConversionSkillTest.Config.class)
@ActiveProfiles("UnitConversionSkillTest")
public class UnitConversionSkillTest {
    private static final Logger log = LoggerFactory.getLogger(UnitConversionSkillTest.class);
    public static final String AVATAR_ID = "anime1";

    @Profile("UnitConversionSkillTest")
    @SpringBootApplication(scanBasePackageClasses = {SkillRepository.class, TaskRepository.class, WordNetConfig.class},
        exclude = {HibernateJpaAutoConfiguration.class, DataSourceAutoConfiguration.class, JmxAutoConfiguration.class,
                CamelAutoConfiguration.class, GroovyTemplateAutoConfiguration.class})
    @Import({LumenCoreConfig.class})
//    @EntityScan(basePackageClasses = QuranChapter.class)
//    @Configuration
//    @ComponentScan(basePackageClasses = {IntentExecutor.class, ThingRepository.class, YagoTypeRepository.class, FactServiceImpl.class})
//    @EnableJpaRepositories(basePackageClasses = QuranChapter.class)
//    @EnableNeo4jRepositories(basePackageClasses = ThingRepository.class)
    public static class Config {
        @Bean
        public Channel<Void> mockChannel() {
//            return new LogChannel();
            return mock(Channel.class, withSettings().verboseLogging());
        }

        @Bean
        public FactService mockFactService() {
            return mock(FactService.class, withSettings()/*.verboseLogging()*/);
        }
    }

    @Inject
    private SkillRepository skillRepo;
    @Inject
    private TaskRepository taskRepo;
    @Inject
    private ScriptRepository scriptRepo;
    @Inject
    private Channel<Void> mockChannel;
    @Inject
    private FactService factService;
    @Inject
    private Provider<InteractionSession> sessionProvider;

    @Test
    public void skillsLoaded() {
        assertThat(skillRepo.getSkills().values(), hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    public void unitConversionSkill() {
        final Skill skill = skillRepo.get("unitConversion");
        skill.resolveIntents(taskRepo, scriptRepo);
        assertThat(skill.getId(), equalTo("unitConversion"));
        assertThat(skill.getActivityRefs(), hasSize(greaterThanOrEqualTo(2)));
        assertThat(skill.getIntents(), hasSize(1));
    }

    @Test
    public void unitConversionInteraction() {
        reset(factService, mockChannel);
        try (final InteractionSession session = sessionProvider.get()) {
            session.getActiveLocales().add(LumenLocale.INDONESIAN);
            session.getActiveLocales().add(Locale.US);
            session.open(null, null);

            session.receiveUtterance(Optional.of(LumenLocale.INDONESIAN), "Berapa 5 km dalam cm?", AVATAR_ID, factService, taskRepo, scriptRepo);
            session.update(mockChannel, null);
//            assertThat(session.get("unitConversion.promptMeasurementToUnit").getState(), equalTo(ActivityState.COMPLETED));

//            session.receiveUtterance(Optional.of(LumenLocale.INDONESIAN), "Al-Kahfi:45", factService, taskRepo, scriptRepo);
//            session.update(mockChannel, null);
//            assertThat(session.get("quran.promptQuranChapterVerse").getState(), equalTo(ActivityState.COMPLETED));

            verify(mockChannel, times(1)).express(any(), any(), any());
        }
    }

}

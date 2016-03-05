package org.lskk.lumen.reasoner.quran;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.LumenCoreConfig;
import org.lskk.lumen.core.LumenLocale;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.activity.ActivityState;
import org.lskk.lumen.reasoner.activity.InteractionSession;
import org.lskk.lumen.reasoner.activity.ScriptRepository;
import org.lskk.lumen.reasoner.activity.TaskRepository;
import org.lskk.lumen.reasoner.nlp.WordNetConfig;
import org.lskk.lumen.reasoner.skill.Skill;
import org.lskk.lumen.reasoner.skill.SkillRepository;
import org.lskk.lumen.reasoner.ux.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by ceefour on 18/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(QuranSkillTest.Config.class)
@ActiveProfiles("QuranSkillTest")
public class QuranSkillTest {
    private static final Logger log = LoggerFactory.getLogger(QuranSkillTest.class);

    @Profile("QuranSkillTest")
    @SpringBootApplication(scanBasePackageClasses = {SkillRepository.class, TaskRepository.class, WordNetConfig.class, QuranChapter.class},
        exclude = {JmxAutoConfiguration.class, CamelAutoConfiguration.class, GroovyTemplateAutoConfiguration.class})
    @Import({LumenCoreConfig.class})
    @EntityScan(basePackageClasses = QuranChapter.class)
//    @Configuration
//    @ComponentScan(basePackageClasses = {IntentExecutor.class, ThingRepository.class, YagoTypeRepository.class, FactServiceImpl.class})
    @EnableJpaRepositories(basePackageClasses = QuranChapter.class)
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

    @Inject
    private QuranChapterRepository quranChapterRepo;
    @Inject
    private QuranVerseRepository quranVerseRepo;
    @Inject
    private LiteralRepository literalRepo;

    @Test
    public void skillsLoaded() {
        assertThat(skillRepo.getSkills().values(), hasSize(greaterThanOrEqualTo(1)));
        skillRepo.resolveIntents(taskRepo);
    }

    @Test
    public void quranSkill() {
        final Skill quranSkill = skillRepo.get("quran");
        assertThat(quranSkill.getId(), equalTo("quran"));
        assertThat(quranSkill.getActivityRefs(), hasSize(greaterThanOrEqualTo(1)));
        assertThat(quranSkill.getIntents(), hasSize(1));
    }

    @Test
    public void quranInteraction() {
        reset(factService, mockChannel);
        try (final InteractionSession session = sessionProvider.get()) {
            session.getActiveLocales().add(LumenLocale.INDONESIAN);
            session.getActiveLocales().add(Locale.US);
            session.open();

            session.receiveUtterance(LumenLocale.INDONESIAN, "baca Quran", factService, taskRepo, scriptRepo);
            session.update(mockChannel);
            assertThat(session.get("quran.promptQuranChapterVerse").getState(), equalTo(ActivityState.ACTIVE));

            session.receiveUtterance(LumenLocale.INDONESIAN, "Al-Kahfi:45", factService, taskRepo, scriptRepo);
            session.update(mockChannel);
            assertThat(session.get("quran.promptQuranChapterVerse").getState(), equalTo(ActivityState.COMPLETED));

            verify(mockChannel, times(2)).express(any(), any(), any());
        }
    }

    @Test
    public void readAlKahfi() {
        log.info("halo");
        final QuranChapter quranChapter = quranChapterRepo.findOne("quran_18");
        log.info("chapter: {}", quranChapter);
    }

    @Test
    public void readAlKahfiAyat46() {
        final QuranVerse quranVerse = quranVerseRepo.findOne("quran_18_verse_46");
        log.info("verse: {}", quranVerse);
    }

    @Test
    public void readAlKahfiAyat46Literal() {
        final Literal quranLiteral = literalRepo.findOne("quran_18_verse_46_ind");
        log.info("literal: {}", quranLiteral);
    }
}

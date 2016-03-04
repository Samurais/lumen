package org.lskk.lumen.reasoner.activity;

import net.sourceforge.plantuml.SourceStringReader;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.LumenCoreConfig;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.skill.Skill;
import org.lskk.lumen.reasoner.skill.SkillRepository;
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
import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by ceefour on 18/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(SkillUmlTest.Config.class)
@ActiveProfiles("SkillUmlTest")
public class SkillUmlTest {
    private static final Logger log = LoggerFactory.getLogger(SkillUmlTest.class);

    @Profile("SkillUmlTest")
    @SpringBootApplication(scanBasePackageClasses = {SkillRepository.class/*, InteractionTaskRepository.class, WordNetConfig.class*/},
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
    private SkillRepository skillRepo;
//    @Inject
//    private InteractionTaskRepository taskRepo;
//    @Inject
//    private Channel<Void> mockChannel;
    @Inject
    private FactService factService;
//    @Inject
//    private Provider<InteractionSession> sessionProvider;

    @Test
    public void skillsLoaded() {
        assertThat(skillRepo.getSkills().values(), hasSize(greaterThanOrEqualTo(1)));
//        skillRepo.resolveIntents(taskRepo);
    }

    @Test
    public void quranSkill() {
        final Skill quranSkill = skillRepo.get("quran");
//        assertThat(quranSkill.getId(), equalTo("quran"));
//        assertThat(quranSkill.getTasks(), hasSize(greaterThanOrEqualTo(1)));
//        assertThat(quranSkill.getIntents(), hasSize(1));
    }

    @Test
    public void unitConversionSkill() throws IOException {
        reset(factService);
        final Skill skill = skillRepo.get("unitConversion");
        final String uml = skill.renderUml();
        log.info("UML:\n{}", uml);

        final SourceStringReader reader = new SourceStringReader(uml);
        final File tempFile = new File(FileUtils.getTempDirectory(), "lumen_skill_unitConversion.png");
        final String generated = reader.generateImage(tempFile);
        log.info("Generated UML Activity Diagram {} to {}", generated, tempFile);
    }

}

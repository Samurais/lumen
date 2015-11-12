package org.lskk.lumen.reasoner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.drools.core.time.SessionPseudoClock;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieSession;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.expression.Proposition;
import org.lskk.lumen.reasoner.expression.SpoNoun;
import org.lskk.lumen.reasoner.goal.TellStory;
import org.lskk.lumen.reasoner.nlp.*;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.lskk.lumen.reasoner.nlp.id.IndonesianSentenceGenerator;
import org.lskk.lumen.reasoner.story.Story;
import org.lskk.lumen.reasoner.story.StoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by ceefour on 27/10/2015.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = StoryTest.Config.class)
public class StoryTest {

    private static final Logger log = LoggerFactory.getLogger(StoryTest.class);
    public static final Locale INDONESIAN = Locale.forLanguageTag("id-ID");

    @Configuration
    @Import({JacksonAutoConfiguration.class, WordNetConfig.class, DroolsTestConfig.class})
    @PropertySource(value = {"classpath:application.properties", "file:config/application.properties"},
            ignoreResourceNotFound = true)
//    @EnableConfigurationProperties
    public static class Config {
        @Bean
        public SentenceGenerator sentenceGenerator_en() {
            return new SentenceGenerator();
        }

        @Bean
        public SentenceGenerator sentenceGenerator_id() {
            return new IndonesianSentenceGenerator();
        }

        @Bean
        public PronounMapper pronounMapper() {
            return new PronounMapper();
        }

        @Bean
        public PreferredMapper preferredMapper() { return new PreferredMapper(); }

        @Bean
        public StoryRepository storyRepo() { return new StoryRepository(); }
    }

    @Inject @NaturalLanguage("en")
    private SentenceGenerator sentenceGenerator_en;
    @Inject @NaturalLanguage("id")
    private SentenceGenerator sentenceGenerator_id;
    @Inject
    private ObjectMapper mapper;
    @Inject
    private KieSession kieSession;

    @Test
    public void story1() throws IOException {
        final SessionPseudoClock clock = kieSession.getSessionClock();
        final TellStory tellStory = new TellStory();
        kieSession.insert(tellStory);
        kieSession.fireAllRules();
        assertThat(tellStory.getStoryId(), Matchers.equalTo("soon_see"));
        log.info("Waiting...");
        clock.advanceTime(4, TimeUnit.SECONDS);
        kieSession.fireAllRules();
        log.info("Waiting...");
        clock.advanceTime(1, TimeUnit.SECONDS);
        kieSession.fireAllRules();
        log.info("Waiting...");
        clock.advanceTime(5, TimeUnit.SECONDS);
        kieSession.fireAllRules();
        log.info("Waiting...");
        clock.advanceTime(5, TimeUnit.SECONDS);
        kieSession.fireAllRules();
        log.info("Waiting...");
        clock.advanceTime(5, TimeUnit.SECONDS);
        kieSession.fireAllRules();
    }

}

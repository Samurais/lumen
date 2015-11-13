package org.lskk.lumen.reasoner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.drools.core.time.SessionPseudoClock;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.api.runtime.KieSession;
import org.lskk.lumen.reasoner.aiml.AimlService;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.lskk.lumen.reasoner.goal.TellStory;
import org.lskk.lumen.reasoner.nlp.NaturalLanguage;
import org.lskk.lumen.reasoner.nlp.PreferredMapper;
import org.lskk.lumen.reasoner.nlp.PronounMapper;
import org.lskk.lumen.reasoner.nlp.WordNetConfig;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.lskk.lumen.reasoner.nlp.id.IndonesianSentenceGenerator;
import org.lskk.lumen.reasoner.story.StoryRepository;
import org.lskk.lumen.reasoner.ux.LogChannel;
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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by ceefour on 27/10/2015.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = StoryAimlTest.Config.class)
public class StoryAimlTest {

    private static final Logger log = LoggerFactory.getLogger(StoryAimlTest.class);
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

        @Bean
        public AimlService aimlService() { return new AimlService(); }

        @Bean
        public DroolsService droolsService() { return new DroolsService(); }

        @Bean
        public LogChannel logChannel() { return new LogChannel(); }
    }

    @Inject @NaturalLanguage("en")
    private SentenceGenerator sentenceGenerator_en;
    @Inject @NaturalLanguage("id")
    private SentenceGenerator sentenceGenerator_id;
    @Inject
    private ObjectMapper mapper;
    @Inject
    private KieSession kieSession;
    @Inject
    private AimlService aimlService;
    @Inject
    private DroolsService droolsService;
    @Inject
    private LogChannel logChannel;

    @Test
    public void storyAiml1() throws IOException {
        AgentResponse resp;
        resp = aimlService.process(Locale.US, "tell me a good story", logChannel);
        assertThat(resp.getInsertables(), not(empty()));
        assertThat(resp.getInsertables(), contains(instanceOf(TellStory.class)));

        droolsService.process(resp);
        final TellStory tellStory = (TellStory) resp.getInsertables().get(0);
        assertThat(kieSession.getFactHandle(tellStory), notNullValue());

        final SessionPseudoClock clock = kieSession.getSessionClock();
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

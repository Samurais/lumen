package org.lskk.lumen.reasoner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.aiml.AimlService;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.lskk.lumen.reasoner.nlp.WordNetConfig;
import org.lskk.lumen.reasoner.quran.ReciteQuran;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.Locale;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Created by ceefour on 27/10/2015.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AimlServiceTest.Config.class)
public class AimlServiceTest {

    private static final Logger log = LoggerFactory.getLogger(AimlServiceTest.class);
    public static final Locale INDONESIAN = Locale.forLanguageTag("id-ID");

    @Configuration
    @Import({JacksonAutoConfiguration.class, WordNetConfig.class})
    @PropertySource(value = {"classpath:application.properties", "file:config/application.properties"},
            ignoreResourceNotFound = true)
//    @EnableConfigurationProperties
    public static class Config {
        @Bean
        public AimlService aimlService() { return new AimlService(); }

    }

    @Inject
    private AimlService aimlService;

    @Test
    public void read() {
        assertThat(aimlService.getAiml().getCategories(), hasSize(greaterThan(5)));
    }

    @Test
    public void match() {
        AimlService.MatchingCategory matching;
        matching = AimlService.match(Locale.US, "good morning", "good morning");
        assertThat(matching.truthValue[1], equalTo(1f));
        matching = AimlService.match(Locale.US, "good morning arkan", "good morning *");
        assertThat(matching.truthValue[1], equalTo(0.91f));
        assertThat(matching.groups, contains("arkan"));
        matching = AimlService.match(Locale.US, "arkan bye", "* bye");
        assertThat(matching.truthValue[1], equalTo(0.81f));
        assertThat(matching.groups, contains("arkan"));
        matching = AimlService.match(Locale.US, "arkan bye", "_ bye");
        assertThat(matching.truthValue[1], equalTo(0.82f));
        assertThat(matching.groups, contains("arkan"));
        matching = AimlService.match(Locale.US, "i love allah", "i love _");
        assertThat(matching.truthValue[1], equalTo(0.92f));
        assertThat(matching.groups, contains("allah"));
        matching = AimlService.match(Locale.US, "i love allah so much", "i love _");
        assertThat(matching.truthValue[1], equalTo(0f));
    }

    @Test
    public void matchTwoGroups() {
        AimlService.MatchingCategory matching;
        matching = AimlService.match(Locale.US, "read al kahfi ayat 42", "read _ _ ayat _");
        log.info("Matching: {}", matching);
        assertThat(matching.truthValue[1], greaterThanOrEqualTo(0.9f));
        assertThat(matching.groups, hasSize(4));
        assertThat(matching.groups, contains("read al kahfi ayat 42", "al", "kahfi", "42"));
    }

    @Test
    public void processStatic() {
        AgentResponse resp;
        // SRAI to "hi", but salutations.aiml has no rule for "hi"
//        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
//        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
        resp = aimlService.process(Locale.US, "hello, how are you??", null, null);
        assertThat(((CommunicateAction) resp.getCommunicateAction()).getObject(), equalTo("I am fine thank you how are you?"));
    }

    @Test
    public void processRandom() {
        AgentResponse resp;
        // SRAI to "hi", but salutations.aiml has no rule for "hi"
//        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
//        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
        resp = aimlService.process(Locale.US, "how are you", null, null);
        assertThat(((CommunicateAction) resp.getCommunicateAction()).getObject(), equalTo("I am fine thank you how are you?"));
    }

    @Test
    public void processImage() {
        AgentResponse resp;
        // SRAI to "hi", but salutations.aiml has no rule for "hi"
//        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
//        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
        resp = aimlService.process(Locale.US, "cat", null, null);
        final CommunicateAction communicateAction = (CommunicateAction) resp.getCommunicateAction();
        assertThat(communicateAction.getObject(), containsString("funny cat"));
        assertThat(communicateAction.getImage(), notNullValue());
    }

    @Test
    public void processGroups() {
        AgentResponse resp;
        // SRAI to "hi", but salutations.aiml has no rule for "hi"
//        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
//        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
        resp = aimlService.process(Locale.US, "read al-kahfi ayat 104", null, null);
        log.info("Response: {}", resp);
        assertThat(resp.getInsertables(), hasSize(1));
        assertThat(resp.getInsertables().get(0), instanceOf(ReciteQuran.class));
        final ReciteQuran reciteQuran = (ReciteQuran) resp.getInsertables().get(0);
        log.info("ReciteQuran: {}", reciteQuran);
        assertThat(reciteQuran.getUpChapter(), equalTo("al-kahfi"));
        assertThat(reciteQuran.getUpVerses(), equalTo("104"));
    }

}

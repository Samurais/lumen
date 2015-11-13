package org.lskk.lumen.reasoner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.aiml.AimlService;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.lskk.lumen.reasoner.nlp.WordNetConfig;
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
        matching = AimlService.match(Locale.US, "GOOD MORNING", "GOOD MORNING");
        assertThat(matching.truthValue[1], equalTo(1f));
        matching = AimlService.match(Locale.US, "GOOD MORNING ARKAN", "GOOD MORNING *");
        assertThat(matching.truthValue[1], equalTo(0.91f));
        assertThat(matching.groups, contains("ARKAN"));
        matching = AimlService.match(Locale.US, "ARKAN BYE", "* BYE");
        assertThat(matching.truthValue[1], equalTo(0.81f));
        assertThat(matching.groups, contains("ARKAN"));
        matching = AimlService.match(Locale.US, "ARKAN BYE", "_ BYE");
        assertThat(matching.truthValue[1], equalTo(0.82f));
        assertThat(matching.groups, contains("ARKAN"));
        matching = AimlService.match(Locale.US, "I LOVE ALLAH", "I LOVE _");
        assertThat(matching.truthValue[1], equalTo(0.92f));
        assertThat(matching.groups, contains("ALLAH"));
        matching = AimlService.match(Locale.US, "I LOVE ALLAH SO MUCH", "I LOVE _");
        assertThat(matching.truthValue[1], equalTo(0f));
    }

    @Test
    public void processStatic() {
        AgentResponse resp;
        // SRAI to "hi", but salutations.aiml has no rule for "hi"
//        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
//        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
        resp = aimlService.process(Locale.US, "hello, how are you??", null);
        assertThat(((CommunicateAction) resp.getCommunicateAction()).getObject(), equalTo("I am fine thank you how are you?"));
    }

    @Test
    public void processRandom() {
        AgentResponse resp;
        // SRAI to "hi", but salutations.aiml has no rule for "hi"
//        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
//        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
        resp = aimlService.process(Locale.US, "how are you", null);
        assertThat(((CommunicateAction) resp.getCommunicateAction()).getObject(), equalTo("I am fine thank you how are you?"));
    }

    @Test
    public void processImage() {
        AgentResponse resp;
        // SRAI to "hi", but salutations.aiml has no rule for "hi"
//        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
//        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
        resp = aimlService.process(Locale.US, "cat", null);
        final CommunicateAction communicateAction = (CommunicateAction) resp.getCommunicateAction();
        assertThat(communicateAction.getObject(), containsString("funny cat"));
        assertThat(communicateAction.getImage(), notNullValue());
    }

}

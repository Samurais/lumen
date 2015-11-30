package org.lskk.lumen.reasoner.aiml;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.core.MediaLayer;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Created by ceefour on 27/10/2015.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IslamicSalutationsTest.Config.class)
public class IslamicSalutationsTest {

    private static final Logger log = LoggerFactory.getLogger(IslamicSalutationsTest.class);

    @Configuration
    @Import({JacksonAutoConfiguration.class, WordNetConfig.class})
    @PropertySource(value = {"classpath:application.properties", "file:config/application.properties"},
            ignoreResourceNotFound = true)
//    @EnableConfigurationProperties
    public static class Config {
        @Bean
        public AimlService aimlService() {
            final AimlService aimlService = new AimlService();
            aimlService.getResourcePatterns().clear();
            aimlService.getResourcePatterns().add("classpath:org/lskk/lumen/reasoner/aiml/arkan/islamic_salutations.aiml");
            return aimlService;
        }

    }

    @Inject
    private AimlService aimlService;

    @Test
    public void processAssalaamualaikum() {
        AgentResponse resp;
        // SRAI to "hi", but salutations.aiml has no rule for "hi"
//        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
//        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
        resp = aimlService.process(Locale.US, "assalaamualaikum", null, null, true);
        final CommunicateAction communicateAction = resp.getCommunicateActions().get(0);
        assertThat(communicateAction, notNullValue());
        log.info("Response's communicate action: {}", communicateAction);
        assertThat(communicateAction.getObject(), startsWith("Wa'alaikumussalaam"));
        assertThat(communicateAction.getInLanguage(), equalTo(AimlService.INDONESIAN));
        assertThat(communicateAction.getUsedForSynthesis(), equalTo(false));
        assertThat(communicateAction.getAudio(), notNullValue());
        assertThat(communicateAction.getAudio().getUrl(), containsString("waalaikumussalam"));
        assertThat(communicateAction.getAudio().getMediaLayer(), equalTo(MediaLayer.SPEECH));
    }

    @Test
    public void processAssalaamualaikumHowAreYou() {
        AgentResponse resp;
        // SRAI to "hi", but salutations.aiml has no rule for "hi"
//        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
//        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
        resp = aimlService.process(Locale.US, "assalaamualaikum how are you", null, null, true);
        log.info("Response: {}", resp);
        assertThat(resp.getCommunicateActions(), hasSize(2));
        final CommunicateAction comm1 = resp.getCommunicateActions().get(0);
        log.info("Response's communicate action[0]: {}", comm1);
        assertThat(comm1.getObject(), startsWith("Alhamdulillah"));
        assertThat(comm1.getInLanguage(), equalTo(AimlService.INDONESIAN));
        assertThat(comm1.getUsedForSynthesis(), equalTo(false));
        assertThat(comm1.getAudio(), notNullValue());
        assertThat(comm1.getAudio().getUrl(), containsString("alhamdulillah"));
        assertThat(comm1.getAudio().getMediaLayer(), equalTo(MediaLayer.SPEECH));
        final CommunicateAction comm2 = resp.getCommunicateActions().get(1);
        log.info("Response's communicate action[1]: {}", comm2);
        assertThat(comm2.getObject(), startsWith("I am fine"));
        assertThat(comm2.getInLanguage(), equalTo(Locale.US));
        assertThat(comm2.getUsedForSynthesis(), equalTo(true));
        assertThat(comm2.getAudio(), nullValue());
    }

    @Test
    public void processApaKabar() {
        AgentResponse resp;
        // SRAI to "hi", but salutations.aiml has no rule for "hi"
//        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
//        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
        resp = aimlService.process(Locale.US, "apa kabar", null, null, true);
        log.info("Response: {}", resp);
        assertThat(resp.getCommunicateActions(), hasSize(2));
        final CommunicateAction comm1 = resp.getCommunicateActions().get(0);
        log.info("Response's communicate action[0]: {}", comm1);
        assertThat(comm1.getObject(), startsWith("Alhamdulillah"));
        assertThat(comm1.getInLanguage(), equalTo(AimlService.INDONESIAN));
        assertThat(comm1.getUsedForSynthesis(), equalTo(false));
        assertThat(comm1.getAudio(), notNullValue());
        assertThat(comm1.getAudio().getUrl(), containsString("alhamdulillah"));
        assertThat(comm1.getAudio().getMediaLayer(), equalTo(MediaLayer.SPEECH));
        final CommunicateAction comm2 = resp.getCommunicateActions().get(1);
        log.info("Response's communicate action[1]: {}", comm2);
        assertThat(comm2.getObject(), startsWith("Aku baik-baik saja"));
        assertThat(comm2.getInLanguage(), equalTo(AimlService.INDONESIAN));
        assertThat(comm2.getUsedForSynthesis(), equalTo(true));
        assertThat(comm2.getAudio(), nullValue());
    }

//    @Test
//    public void processRandom() {
//        AgentResponse resp;
//        // SRAI to "hi", but salutations.aiml has no rule for "hi"
////        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
////        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
//        resp = aimlService.process(Locale.US, "how are you", null, null);
//        assertThat(((CommunicateAction) resp.getCommunicateAction()).getObject(), equalTo("I am fine thank you how are you?"));
//    }
//
//    @Test
//    public void processImage() {
//        AgentResponse resp;
//        // SRAI to "hi", but salutations.aiml has no rule for "hi"
////        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
////        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
//        resp = aimlService.process(Locale.US, "cat", null, null);
//        final CommunicateAction communicateAction = (CommunicateAction) resp.getCommunicateAction();
//        assertThat(communicateAction.getObject(), containsString("funny cat"));
//        assertThat(communicateAction.getImage(), notNullValue());
//    }
//
//    @Test
//    public void processGroups() {
//        AgentResponse resp;
//        // SRAI to "hi", but salutations.aiml has no rule for "hi"
////        resp = aimlService.process(Locale.US, "konnichiwa ... !!");
////        assertThat(((CommunicateAction) resp.getResponse()).getObject(), equalTo("hello"));
//        resp = aimlService.process(Locale.US, "read al-kahfi ayat 104", null, null);
//        log.info("Response: {}", resp);
//        assertThat(resp.getInsertables(), hasSize(1));
//        assertThat(resp.getInsertables().get(0), instanceOf(ReciteQuran.class));
//        final ReciteQuran reciteQuran = (ReciteQuran) resp.getInsertables().get(0);
//        log.info("ReciteQuran: {}", reciteQuran);
//        assertThat(reciteQuran.getUpChapter(), equalTo("al-kahfi"));
//        assertThat(reciteQuran.getUpVerses(), equalTo("104"));
//    }

}

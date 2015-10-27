package org.lskk.lumen.reasoner;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.expression.SpoNoun;
import org.lskk.lumen.reasoner.nlp.*;
import org.lskk.lumen.reasoner.nlp.en.IndonesianSentenceGenerator;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Created by ceefour on 27/10/2015.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SentenceGeneratorTest.Config.class)
public class SentenceGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(SentenceGeneratorTest.class);
    public static final Locale INDONESIAN = Locale.forLanguageTag("id-ID");

    @Configuration
    @Import({JacksonAutoConfiguration.class, WordNetConfig.class})
    @PropertySource(value = "config/application.properties", ignoreResourceNotFound = true)
    @PropertySource("classpath:application.properties")
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
    }

    @Inject @NaturalLanguage("en")
    private SentenceGenerator sentenceGenerator_en;
    @Inject @NaturalLanguage("id")
    private SentenceGenerator sentenceGenerator_id;

    @Test
    public void spoNoun() {
        generate(new SpoNoun(NounClause.SHE, new Verb("wn30:wordnet_eat_201168468"),
                        new NounClause("yago:wordnet_rice_107804323")),
                "she eats rice", "dia makan nasi");
        generate(new SpoNoun(NounClause.I, new Verb("wn30:wordnet_love_201775535"), NounClause.YOU),
            "I love you", "aku cinta kamu");
        generate(new SpoNoun(NounClause.HE, new Verb("wn30:wordnet_love_201775535"), NounClause.I),
            "he loves me", "dia cinta aku");
    }

    protected void generate(Object sentence, String english, String indonesian) {
        final CommunicateAction action_en = sentenceGenerator_en.generate(Locale.US,
                sentence);
        final CommunicateAction action_id = sentenceGenerator_id.generate(INDONESIAN,
                sentence);
        log.info("Expression: {}", sentence);
        log.info("  en: {}", action_en.getObject());
        log.info("  id: {}", action_id.getObject());
        assertEquals(english, action_en.getObject());
        assertEquals(indonesian, action_id.getObject());
    }

    @Test
    public void makeSentence() {
        assertEquals("I love it!",
                sentenceGenerator_en.makeSentence(ImmutableList.of("I love it"), SentenceMood.EXCLAMATION));
        assertEquals("He doesn't like me...",
                sentenceGenerator_en.makeSentence(ImmutableList.of("he doesn't like me"), SentenceMood.DANGLING));
    }

}

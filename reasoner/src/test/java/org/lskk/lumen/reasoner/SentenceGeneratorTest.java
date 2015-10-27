package org.lskk.lumen.reasoner;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.reasoner.expression.SpoNoun;
import org.lskk.lumen.reasoner.nlp.*;
import org.lskk.lumen.reasoner.nlp.en.IndonesianSentenceGenerator;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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

    public static final Locale INDONESIAN = Locale.forLanguageTag("id-ID");

    @Configuration
    @Import(JacksonAutoConfiguration.class)
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
        generate(new SpoNoun(NounClause.I, new Verb("love"), NounClause.YOU),
            "I love you", "aku cinta kamu");
        generate(new SpoNoun(NounClause.HE, new Verb("love"), NounClause.I),
            "he loves me", "dia cinta aku");
    }

    protected void generate(Object sentence, String english, String indonesian) {
        assertEquals(english, sentenceGenerator_en.generate(Locale.US,
                sentence).getObject());
        assertEquals(indonesian, sentenceGenerator_id.generate(INDONESIAN,
                sentence).getObject());
    }

    @Test
    public void makeSentence() {
        assertEquals("I love it!",
                sentenceGenerator_en.makeSentence(ImmutableList.of("I love it"), SentenceMood.EXCLAMATION));
        assertEquals("He doesn't like me...",
                sentenceGenerator_en.makeSentence(ImmutableList.of("he doesn't like me"), SentenceMood.DANGLING));
    }

}

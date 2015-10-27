package org.lskk.lumen.reasoner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.reasoner.expression.SpoNoun;
import org.lskk.lumen.reasoner.nlp.NounClause;
import org.lskk.lumen.reasoner.nlp.PronounMapper;
import org.lskk.lumen.reasoner.nlp.Verb;
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

    @Configuration
    @Import(JacksonAutoConfiguration.class)
    public static class Config {
        @Bean
        public SentenceGenerator sentenceGenerator() {
            return new SentenceGenerator();
        }

        @Bean
        public PronounMapper pronounMapper() {
            return new PronounMapper();
        }
    }

    @Inject
    private SentenceGenerator sentenceGenerator;

    @Test
    public void spoNoun() {
        assertEquals("I love you", sentenceGenerator.generate(Locale.US,
                new SpoNoun(NounClause.I, new Verb("love"), NounClause.YOU)).getObject());
        assertEquals("he loves me", sentenceGenerator.generate(Locale.US,
                new SpoNoun(NounClause.HE, new Verb("love"), NounClause.I)).getObject());
    }


}

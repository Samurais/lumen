package org.lskk.lumen.reasoner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.expression.Proposition;
import org.lskk.lumen.reasoner.expression.SpoNoun;
import org.lskk.lumen.reasoner.nlp.*;
import org.lskk.lumen.reasoner.nlp.id.IndonesianSentenceGenerator;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
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
    }

    @Inject @NaturalLanguage("en")
    private SentenceGenerator sentenceGenerator_en;
    @Inject @NaturalLanguage("id")
    private SentenceGenerator sentenceGenerator_id;
    @Inject
    private ObjectMapper mapper;

    @Test
    public void spoNoun() {
        assertSentence(new SpoNoun(NounClause.THEY, new Verb("wn30:wordnet_drive_201930874"),
                        new NounClause("yago:wordnet_car_102958343", NounClause.SHE)),
                "they drive her car", "mereka mengendarai mobil dia");
        assertSentence(new SpoNoun(NounClause.SHE, new Verb("wn30:wordnet_eat_201168468"),
                        new NounClause("yago:wordnet_rice_107804323")),
                "she eats rice", "dia makan nasi");
        assertSentence(new SpoNoun(NounClause.I, new Verb("wn30:wordnet_love_201775535"), NounClause.YOU),
                "I love you", "aku cinta kamu");
        assertSentence(new SpoNoun(NounClause.HE, new Verb("wn30:wordnet_love_201775535"), NounClause.I),
                "he loves me", "dia cinta aku");
    }

    protected void assertSentence(Proposition sentence, String english, String indonesian) {
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

    @Test
    public void story1() throws IOException {
        final List<Story> stories = mapper.readValue(SentenceGeneratorTest.class.getResource("stories.json"),
                new TypeReference<List<Story>>() {});
        assertSentence(stories.get(0).getPropositions().get(0),
                "I should go to the zoo", "aku sebaiknya pergi ke kebun binatang");
    }

}

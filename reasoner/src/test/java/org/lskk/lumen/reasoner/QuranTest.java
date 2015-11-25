package org.lskk.lumen.reasoner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.reasoner.aiml.AimlService;
import org.lskk.lumen.reasoner.nlp.WordNetConfig;
import org.lskk.lumen.reasoner.quran.QuranChapter;
import org.lskk.lumen.reasoner.quran.QuranChapterRepository;
import org.lskk.lumen.reasoner.quran.QuranVerse;
import org.lskk.lumen.reasoner.quran.QuranVerseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

/**
 * Created by aina on 18/11/2015.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = QuranTest.Config.class)
public class QuranTest {

    private static final Logger log = LoggerFactory.getLogger(QuranTest.class);

    @Configuration
    @Import({JacksonAutoConfiguration.class, DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
    @EntityScan("org.lskk.lumen.reasoner.quran")
    @EnableJpaRepositories
    @PropertySource(value = {"classpath:application.properties", "file:config/application.properties"},
            ignoreResourceNotFound = true)
    //@ComponentScan("org.lskk.lumen.reasoner.quran")
//    @EnableConfigurationProperties
    public static class Config {


    }

    @Inject
    private QuranChapterRepository quranChapterRepo;
    @Inject
    private QuranVerseRepository quranVerseRepo;

    @Test
    public void readAlKahfi() {
        log.info("halo");
        final QuranChapter quranChapter = quranChapterRepo.findOne("quran_18");
        log.info("chapter: {}", quranChapter);
    }

    @Test
    public void readAlKahfiAyat46() {
        final QuranVerse quranVerse = quranVerseRepo.findOne("quran_18_verse_46");
        log.info("verse: {}", quranVerse);
    }

    //@Test
    //public void readAlKahfiAyat46Literal() {
      //  final QuranVerse quranliteral = literalRepo.findOne("quran_18_verse_46");
        // log.info("verse:{}", quranVerse);
    //}
}

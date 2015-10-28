package org.lskk.lumen.reasoner;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.reasoner.aiml.AimlService;
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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

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
    public void simple() {
        Assert.assertThat(aimlService.getAiml().getCategories(), hasSize(greaterThan(5)));
    }

}

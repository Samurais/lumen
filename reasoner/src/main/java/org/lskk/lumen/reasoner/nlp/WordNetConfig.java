package org.lskk.lumen.reasoner.nlp;

import edu.mit.jwi.CachingDictionary;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.ICachingDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

/**
 * Created by ceefour on 27/10/2015.
 */
@Configuration
public class WordNetConfig {
    private static final Logger log = LoggerFactory.getLogger(WordNetConfig.class);
    @Inject
    private Environment env;

    @Bean(destroyMethod = "close")
    @NaturalLanguage("en")
    public ICachingDictionary wordNet() throws IOException {
        final File wordNetFolder = env.getRequiredProperty("wordnet.folder", File.class);
        final Dictionary backing = new Dictionary(wordNetFolder);
        final CachingDictionary dict = new CachingDictionary(backing);
        dict.open();
        log.info("Loaded WordNet {} dictionary from {}", dict.getVersion(), wordNetFolder);
        return dict;
    }

}

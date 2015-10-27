package org.lskk.lumen.reasoner.nlp;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.opencsv.CSVReader;
import edu.mit.jwi.CachingDictionary;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.ICachingDictionary;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.io.*;

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
        final File dictFolder = new File(env.getRequiredProperty("wordnet.folder", File.class), "dict");
        final Dictionary backing = new Dictionary(dictFolder);
        final CachingDictionary dict = new CachingDictionary(backing);
        dict.open();
        log.info("Loaded WordNet {} dictionary from {}", dict.getVersion(), dictFolder);
        return dict;
    }

    @Bean @NaturalLanguage("id")
    public Multimap<String, String> wordNetInd() throws IOException {
        final ImmutableMultimap.Builder<String, String> multimab = ImmutableMultimap.builder();
        final File indFile = new File(env.getRequiredProperty("wordnet.folder", File.class), "msa/wn-data-ind.tab");
        log.info("Loading Indonesian WordNet from {} ...", indFile);
        try (CSVReader csv = new CSVReader(new FileReader(indFile), '\t', '"', 1)) {
            while (true) {
                final String[] line = csv.readNext();
                if (line == null) {
                    break;
                }
                final String synsetId = line[0];
                final String lemma = line[2];
                multimab.put(synsetId, lemma);
            }
        }
        final ImmutableMultimap<String, String> multimap = multimab.build();
        log.info("Loaded Indonesian WordNet with {} synsets, total {} lemmas", multimap.keySet().size(),
                multimap.size());
        return multimap;
    }

}

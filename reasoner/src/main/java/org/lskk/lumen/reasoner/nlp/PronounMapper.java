package org.lskk.lumen.reasoner.nlp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ceefour on 27/10/2015.
 */
@Service
public class PronounMapper {

    private static final Logger log = LoggerFactory.getLogger(PronounMapper.class);
    private Map<Locale, Map<Pronoun, String>> langPronouns = new ConcurrentHashMap<>();

    @Inject
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws IOException {
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(PronounMapper.class.getClassLoader());
        final Resource[] resources = resolver.getResources("classpath:org/lskk/lumen/reasoner/nlp/pronoun.*.json");
        for (final Resource res : resources) {
            final Locale lang = Locale.forLanguageTag(StringUtils.substringBetween(res.getFilename(), "pronoun.", ".json"));
            final Map<Pronoun, String> pronouns = objectMapper.readValue(res.getURL(), new TypeReference<Map<Pronoun, String>>() { });
            log.info("Read {} {} pronouns from {}", pronouns.size(), lang, res);
            langPronouns.put(lang, pronouns);
        }
        log.info("Supports {} languages for pronoun mapping: {}", langPronouns.size(), langPronouns.keySet());
    }

    public Optional<String> getPronounLabel(Locale locale, Pronoun pronoun) {
        if (langPronouns.get(locale) != null) {
            return Optional.ofNullable(langPronouns.get(locale).get(pronoun));
        } else {
            return Optional.empty();
        }
    }

}

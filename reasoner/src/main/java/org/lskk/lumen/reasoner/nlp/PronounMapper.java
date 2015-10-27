package org.lskk.lumen.reasoner.nlp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.reasoner.ReasonerException;
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
    private Map<Locale, Map<Pronoun, PronounLabel>> langPronouns = new ConcurrentHashMap<>();

    @Inject
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws IOException {
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(PronounMapper.class.getClassLoader());
        final Resource[] resources = resolver.getResources("classpath:org/lskk/lumen/reasoner/nlp/pronoun.*.json");
        for (final Resource res : resources) {
            final Locale lang = Locale.forLanguageTag(StringUtils.substringBetween(res.getFilename(), "pronoun.", ".json"));
            final Map<Pronoun, PronounLabel> pronouns = objectMapper.readValue(res.getURL(), new TypeReference<Map<Pronoun, PronounLabel>>() { });
            log.info("Read {} {} pronouns from {}", pronouns.size(), lang, res);
            langPronouns.put(lang, pronouns);
        }
        log.info("Supports {} languages for pronoun mapping: {}", langPronouns.size(), langPronouns.keySet());
    }

    public Optional<String> getPronounLabel(Locale locale, Pronoun pronoun, PronounCase pronounCase) {
        if (langPronouns.get(locale) != null) {
            return Optional.ofNullable(langPronouns.get(locale).get(pronoun)).map(it -> {
                switch (pronounCase) {
                    case SUBJECT: return it.getSubject();
                    case OBJECT: return it.getObject();
                    case POSSESSIVE_ADJ: return it.getPossessiveAdj();
                    case POSSESSIVE_PRONOUN: return it.getPossessivePronoun();
                    case REFLEXIVE: return it.getReflexive();
                    default:
                        throw new ReasonerException("Unknown pronoun case: " + pronounCase);
                }
            });
        } else {
            return Optional.empty();
        }
    }

}

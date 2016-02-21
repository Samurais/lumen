package org.lskk.lumen.persistence.service;

import com.google.common.collect.Ordering;
import org.apache.camel.language.Simple;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.lskk.lumen.persistence.LumenPersistenceException;
import org.lskk.lumen.persistence.jpa.YagoLabel;
import org.lskk.lumen.persistence.jpa.YagoType;
import org.lskk.lumen.persistence.jpa.YagoTypeRepository;
import org.lskk.lumen.persistence.neo4j.PartitionKey;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.neo4j.ThingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 14/02/2016.
 */
@Service
public class FactServiceImpl implements FactService {

    private static final Logger log = LoggerFactory.getLogger(FactServiceImpl.class);

    @Inject
    private ThingRepository thingRepo;
    @Inject
    private YagoTypeRepository yagoTypeRepo;

    protected static float getMatchingThingConfidence(String upLabel, Locale inLanguage,
                                               List<YagoLabel> labels) {
        if (labels.isEmpty()) {
            return 0f;
        }
        int leastLev = Integer.MAX_VALUE;
        YagoLabel bestLabel = null;
        for (final YagoLabel label : labels) {
            final int lev = StringUtils.getLevenshteinDistance(upLabel, label.getValue());
            if (bestLabel == null || lev < leastLev) {
                bestLabel = label;
                leastLev = lev;
            }
        }
        final double inverseLevenshteinMultiplier = Math.max((-Math.exp(0.25 * leastLev) + 11d) / 10d, 0d);
        final double languageMultiplier;
        if (bestLabel.getInLanguage() != null) {
            final Locale labelLang = Locale.forLanguageTag(bestLabel.getInLanguage());
            if (inLanguage.equals(labelLang)) {
                languageMultiplier = 1d;
               } else if (inLanguage.getLanguage().equals(labelLang.getLanguage())) {
                languageMultiplier = 0.9d;
            } else {
                languageMultiplier = 0.5d;
            }
        } else {
            languageMultiplier = 0.5d;
        }
        final float confidence = (float) (inverseLevenshteinMultiplier * languageMultiplier);
        return confidence;
    }

    /**
     * Confidence is calculated as follows:
     *
     * inverseLevenshteinMultiplier = max( (-e^(0.25 * levenshtein[10])+11)/10, 0)
     * languageMultiplier = {matches: 1, partial match: 0.9, not match/unknown: 0.5}
     *
     * @param upLabel Label to match, free-form. Fuzzy string matching will also be attempted,
     *                which will be reflected in {@link MatchingThing#getConfidence()}.
     * @param inLanguage Language of the provided label.
     * @param contexts Contexts of the match. Key is node name, value is non-normalized confidence [0..1].
     * @return
     */
    @Override
    public List<MatchingThing> match(String upLabel, Locale inLanguage, Map<String, Float> contexts) {
        final List<MatchingThing> results = new ArrayList<>();

        // TODO: use metaphone

        final List<Thing> neo4jThings = thingRepo.findAllByPrefLabelOrIsPreferredMeaningOf(upLabel);
        neo4jThings.stream().map(it -> {
            final ArrayList<YagoLabel> labels = new ArrayList<>();
            if (null != it.getPrefLabel()) {
                labels.add(new YagoLabel(it.getPrefLabel(), it.getPrefLabelLang()));
            }
            final float confidence = getMatchingThingConfidence(upLabel, inLanguage, labels);
            return new MatchingThing(it, confidence);
        }).forEach(results::add);

        // find matches from YagoType
        final List<YagoType> yagoTypes = yagoTypeRepo.findAllByPrefLabelOrIsPreferredMeaningOfEager(upLabel);
        yagoTypes.stream().map(it -> {
            final ArrayList<YagoLabel> labels = new ArrayList<>();
            if (null != it.getPrefLabel()) {
                labels.add(new YagoLabel(it.getPrefLabel(), Locale.US.toLanguageTag()));
            }
            if (null != it.getIsPreferredMeaningOf()) {
                labels.add(new YagoLabel(it.getIsPreferredMeaningOf(), Locale.US.toLanguageTag()));
            }
            final float confidence = getMatchingThingConfidence(upLabel, inLanguage, labels);
            return new MatchingThing(it.toThingFull(), confidence);
        }).forEach(results::add);

        // Sort results based on confidence
        final List<MatchingThing> sortedResults = Ordering.natural().immutableSortedCopy(results)
                .stream().limit(MAX_RESULTS).collect(Collectors.toList());
        log.info("match() returned {} matches using ({}, {}, {}) => {}",
                sortedResults.size(), upLabel, inLanguage, contexts, sortedResults);

        return sortedResults;
    }

    @Override
    public Thing describeThing(String nodeName) {
        Thing result = null;
        final Thing varThing = thingRepo.findOneByPartitionAndNn(PartitionKey.lumen_var, nodeName);
        if (varThing != null) {
            result = varThing;
        } else {
            final YagoType yagoType = yagoTypeRepo.findOneByNn(nodeName);
            if (null != yagoType) {
                result = yagoType.toThingFull();
            }
        }
        if (null == result) {
            throw new LumenPersistenceException(String.format("Cannot find thing '%s'", nodeName));
        }
        return result;
    }

    @Override
    public Thing assertThing(String nodeName, String upLabel, Locale inLanguage, boolean isPrefLabel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Thing unassertThing(String nodeName) {
        return null;
    }

    @Override
    public Thing assertPropertyToThing(String nodeName, String property, String objectNodeName, float[] truthValue, DateTime assertionTime, String asserterNodeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Fact getProperty(@Simple("body.nodeName") String nodeName, @Simple("body.property") String property) {
        throw new UnsupportedOperationException();
    }
}

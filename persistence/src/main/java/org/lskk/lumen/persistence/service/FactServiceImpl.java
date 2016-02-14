package org.lskk.lumen.persistence.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.lskk.lumen.persistence.jpa.YagoLabel;
import org.lskk.lumen.persistence.jpa.YagoTypeRepository;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.neo4j.ThingRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

/**
 * Created by ceefour on 14/02/2016.
 */
@Service
public class FactServiceImpl implements FactService {

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
        final double inverseLevenshteinMultiplier = Math.min((-Math.exp(0.25 * leastLev) + 11d) / 10d, 0d);
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
     * inverseLevenshteinMultiplier = min( (-e^(0.25 * levenshtein)+11)/10, 0)
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
        final List<Thing> neo4jThings = thingRepo.findAllByPrefLabelOrIsPreferredMeaningOf(upLabel);
        neo4jThings.stream().map(it -> {
            final ArrayList<YagoLabel> labels = new ArrayList<>();
            if (null != it.getPrefLabel()) {
                labels.add(new YagoLabel(it.getPrefLabel(), it.getPrefLabelLang()));
            }
            final float confidence = getMatchingThingConfidence(upLabel, inLanguage, labels);
            return new MatchingThing(it, confidence);
        });

        // TODO: find matches from YagoType

        // Sort results based on confidence
        final ImmutableList<MatchingThing> sortedResults = Ordering.natural().immutableSortedCopy(results);

        return sortedResults;
    }

    @Override
    public Thing describeThing(String nodeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Thing assertThing(String nodeName, String upLabel, Locale inLanguage, boolean isPrefLabel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Thing assertPropertyToThing(String nodeName, String property, String objectNodeName, float[] truthValue, DateTime assertionTime, String asserterNodeName) {
        throw new UnsupportedOperationException();
    }
}

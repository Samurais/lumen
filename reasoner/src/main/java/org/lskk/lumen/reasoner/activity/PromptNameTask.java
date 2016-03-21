package org.lskk.lumen.reasoner.activity;

import org.lskk.lumen.core.IConfidence;
import org.lskk.lumen.persistence.neo4j.PartitionKey;
import org.lskk.lumen.persistence.neo4j.ThingLabel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Specialized {@link PromptTask} that can understand
 * {@link org.apache.jena.vocabulary.SKOS#prefLabel}, {@link org.apache.jena.vocabulary.RDFS#label},
 * {@code yago:isPreferredMeaningOf},
 * {@code yago:hasGivenName}, {@code yago:hasFamilyName}
 * Created by ceefour on 25/02/2016.
 */
public class PromptNameTask extends PromptTask {

    /**
     * Given input utterance, determine the labels that should be given to the Person.
     * The {@link org.apache.jena.vocabulary.SKOS#prefLabel} and {@code yago:isPreferredMeaningOf}
     * will have a {@link ThingLabel#getConfidence()} from raw confidence multiplied by 0.9f, others will have from raw confidence multiplied by 1.0f.
     * @return
     * @param utteranceMatches
     */
    @Override
    public List<ThingLabel> generateLabelsToAssert(List<UtterancePattern> utteranceMatches) {
        final List<ThingLabel> matchedLabels = utteranceMatches.stream().flatMap(utterancePattern -> {
            final List<ThingLabel> outLabels = new ArrayList<>();
            final String name = utterancePattern.getSlotStrings().get("name");

            outLabels.add(new ThingLabel(PartitionKey.lumen_var, utterancePattern.getInLanguage(),
                    name, "rdfs:label", utterancePattern.getConfidence() * 1f));
            outLabels.add(new ThingLabel(PartitionKey.lumen_var, utterancePattern.getInLanguage(),
                    name, "skos:prefLabel", utterancePattern.getConfidence() * 0.9f));
            outLabels.add(new ThingLabel(PartitionKey.lumen_var, utterancePattern.getInLanguage(),
                    name, "yago:isPreferredMeaningOf", utterancePattern.getConfidence() * 0.9f));

            final String[] nameParts = TOKENIZER_ENGLISH.tokenize(name);
            if (nameParts.length >= 2) {
                outLabels.add(new ThingLabel(PartitionKey.lumen_var, utterancePattern.getInLanguage(),
                        Stream.of(Arrays.copyOf(nameParts, nameParts.length - 1)).collect(Collectors.joining(" ")),
                        "yago:hasGivenName", utterancePattern.getConfidence() * 1f));
                outLabels.add(new ThingLabel(PartitionKey.lumen_var, utterancePattern.getInLanguage(),
                        nameParts[nameParts.length - 1],
                        "yago:hasFamilyName", utterancePattern.getConfidence() * 1f));
            } else {
                // only 1 part, assume givenName
                outLabels.add(new ThingLabel(PartitionKey.lumen_var, utterancePattern.getInLanguage(),
                        nameParts[0],
                        "yago:hasGivenName", utterancePattern.getConfidence() * 1f));
            }
            return outLabels.stream();
        })
                .sorted(new IConfidence.Comparator())
                .collect(Collectors.toList());
        log.debug("Generated Labels: {}", matchedLabels);
        return matchedLabels;
    }

}

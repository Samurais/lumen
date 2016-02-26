package org.lskk.lumen.reasoner.interaction;

import com.google.common.collect.ImmutableList;
import org.joda.time.LocalDate;
import org.lskk.lumen.core.ConversationStyle;
import org.lskk.lumen.core.IConfidence;
import org.lskk.lumen.persistence.neo4j.Literal;
import org.lskk.lumen.persistence.neo4j.PartitionKey;
import org.lskk.lumen.persistence.neo4j.SemanticProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Prompts for age or birth year.
 * If the user answers with age, the generated fact should have a confidence of < 1.0.
 * TODO: also support answering birthYear directly, to get confidence of 1.0
 * Created by ceefour on 26/02/2016.
 */
public class PromptAgeTask extends PromptTask {

    /**
     * Only picks the single highest matching utterance pattern.
     * @param utteranceMatches
     * @return
     */
    @Override
    public List<Literal> getLiteralsToAssert(List<UtterancePattern> utteranceMatches) {
        final Optional<UtterancePattern> probableBirthYear = utteranceMatches.stream()
                .max(new IConfidence.Comparator());
        return probableBirthYear.map(utterancePattern -> {
            final Literal literal = new Literal();
            literal.setPartition(PartitionKey.lumen_var);
            literal.setPredicate(SemanticProperty.forLumen("lumen:hasBirthYear"));
            literal.setType("xsd:integer");
            // Literal to assert is birth year, not the "current age".
            // That's why confidence is not 1.0
            final Integer age = (Integer) utterancePattern.getSlotValues().get("age");
            literal.setValue(new LocalDate().getYear() - age);
            literal.setConfidence(0.9f);
            return literal;
        }).map(ImmutableList::of).orElse(ImmutableList.of());
    }
}

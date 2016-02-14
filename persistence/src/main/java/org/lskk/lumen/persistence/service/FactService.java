package org.lskk.lumen.persistence.service;

import org.joda.time.DateTime;
import org.lskk.lumen.persistence.neo4j.Thing;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by ceefour on 14/02/2016.
 */
public interface FactService {
    /**
     * Matches {@code upLabel} with known {@link org.lskk.lumen.persistence.neo4j.Thing}s based on active contexts.
     * @param upLabel Label to match, free-form. Fuzzy string matching will also be attempted,
     *                which will be reflected in {@link MatchingThing#getConfidence()}.
     * @param inLanguage Language of the provided label.
     * @param contexts Contexts of the match. Key is node name, value is non-normalized confidence [0..1].
     * @return
     */
    List<MatchingThing> match(String upLabel, Locale inLanguage, Map<String, Float> contexts);

    /**
     * Describes a thing.
     * @param nodeName
     * @return
     */
    Thing describeThing(String nodeName);

    /**
     * Asserts a thing and gives it a preferred label.
     * @param nodeName
     * @param upLabel
     * @param inLanguage Language of the {@code upLabel}.
     * @param isPrefLabel If {@code true}, this will replace the {@link Thing}'s current {@code prefLabel}.
     *                    Otherwise, will just add a new {@link org.lskk.lumen.persistence.jpa.YagoLabel}.
     */
    Thing assertThing(String nodeName, String upLabel, Locale inLanguage,
                     boolean isPrefLabel);

    /**
     * Assert a property to another thing, i.e. fact.
     * @param nodeName Subject's node name, e.g. {@code lumen:Hendy_Irawan}.
     * @param property Property's QName, e.g. {@code rdf:type}.
     * @param objectNodeName Object's node name, e.g. {@code yago:wordnet_person_100007846}.
     * @param truthValue Truth value of the assertion, relative to asserter.
     * @param assertionTime Time of the assertion.
     * @param asserterNodeName Node name of the person who asserts this.
     * @return
     */
    Thing assertPropertyToThing(String nodeName, String property, String objectNodeName,
                                float[] truthValue, DateTime assertionTime, String asserterNodeName);
}

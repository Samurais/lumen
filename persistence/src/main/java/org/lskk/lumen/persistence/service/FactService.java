package org.lskk.lumen.persistence.service;

import org.apache.camel.language.Simple;
import org.joda.time.DateTime;
import org.lskk.lumen.persistence.neo4j.Literal;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.neo4j.ThingLabel;

import java.util.Locale;
import java.util.Map;

/**
 * Only return DTOs, do not return collection object because {@link org.apache.camel.TypeConverter} cannot properly
 * support them due to insufficient type information.
 * Created by ceefour on 14/02/2016.
 */
public interface FactService {

    int MAX_RESULTS = 100;

    /**
     * Matches {@code upLabel} with known {@link org.lskk.lumen.persistence.neo4j.Thing}s based on active contexts.
     * @param upLabel Label to match, free-form. Fuzzy string matching will also be attempted,
     *                which will be reflected in {@link MatchingThing#getConfidence()}.
     * @param inLanguage Language of the provided label.
     * @param contexts Contexts of the match. Key is node name, value is non-normalized confidence [0..1].
     * @return Matching things, sorted descending by confidence, limited to {@link #MAX_RESULTS}.
     */
    MatchingThings match(@Simple("body.upLabel") String upLabel,
                         @Simple("body.inLanguage") Locale inLanguage,
                         @Simple("body.contexts") Map<String, Float> contexts);

    /**
     * Describes a thing.
     * @param nodeName
     * @return
     */
    Thing describeThing(@Simple("body.nodeName") String nodeName);

    /**
     * Asserts a thing and gives it a preferred label.
     * @param nodeName
     * @param upLabel
     * @param inLanguage Language of the {@code upLabel}.
     * @param isPrefLabel If {@code true}, this will replace the {@link Thing}'s current {@code prefLabel}.
     *                    Otherwise, will just add a new {@link org.lskk.lumen.persistence.jpa.YagoLabel}.
     */
    Thing assertThing(@Simple("body.nodeName") String nodeName, @Simple("body.upLabel") String upLabel, @Simple("body.inLanguage") Locale inLanguage,
                      @Simple("body.isPrefLabel") boolean isPrefLabel);

    /**
     * Unasserts a thing.
     * @param nodeName
     */
    Thing unassertThing(@Simple("body.nodeName") String nodeName);

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
    Thing assertPropertyToThing(@Simple("body.nodeName") String nodeName, @Simple("body.property") String property,
                                @Simple("body.objectNodeName") String objectNodeName,
                                @Simple("body.truthValue") float[] truthValue, @Simple("body.assertionTime") DateTime assertionTime,
                                @Simple("body.asserterNodeName") String asserterNodeName);

    /**
     * Assert a property to literal.
     * @param nodeName Subject's node name, e.g. {@code lumen:Hendy_Irawan}.
     * @param property Property's QName, e.g. {@code rdf:type}.
     * @param objectType Literal type, e.g. {@code xsd:string}.
     * @param object Literal object, can be string or integer or boolean or float.
     * @param truthValue Truth value of the assertion, relative to asserter.
     * @param assertionTime Time of the assertion.
     * @param asserterNodeName Node name of the person who asserts this.
     * @return
     */
    Literal assertPropertyToLiteral(
            @Simple("body.nodeName") String nodeName, @Simple("body.property") String property,
            @Simple("body.objectType") String objectType,
            @Simple("body.object") Object object,
            @Simple("body.truthValue") float[] truthValue, @Simple("body.assertionTime") DateTime assertionTime,
            @Simple("body.asserterNodeName") String asserterNodeName);

    /**
     * Assert a property to label.
     * @param nodeName Subject's node name, e.g. {@code lumen:Hendy_Irawan}.
     * @param property Property's QName, e.g. {@code rdf:type}.
     * @param label Label.
     * @param inLanguage
     * @param truthValue Truth value of the assertion, relative to asserter.
     * @param assertionTime Time of the assertion.
     * @param asserterNodeName Node name of the person who asserts this.    @return
     * */
    ThingLabel assertLabel(
            @Simple("body.nodeName") String nodeName, @Simple("body.property") String property,
            @Simple("body.label") String label, @Simple("body.inLanguage") String inLanguage,
            @Simple("body.truthValue") float[] truthValue, @Simple("body.assertionTime") DateTime assertionTime,
            @Simple("body.asserterNodeName") String asserterNodeName);

    /**
     * Get a single fact or literal property.
     */
    Fact getProperty(@Simple("body.nodeName") String nodeName, @Simple("body.property") String property);
}

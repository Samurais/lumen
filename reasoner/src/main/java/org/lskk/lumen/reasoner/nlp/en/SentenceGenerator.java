package org.lskk.lumen.reasoner.nlp.en;

import com.google.common.base.Preconditions;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.SynsetID;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.expression.*;
import org.lskk.lumen.reasoner.nlp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Natural language generation.
 *
 * The process of language generation involves a series of stages, which may be defined in various ways, such as:
 * <ol>
 *     <li>Content determination: figuring out what needs to be said in a given context</li>
 *     <li>Discourse planning: overall organization of the information to be communicated</li>
 *     <li>Lexicalization: assigning words to concepts</li>
 *     <li>Reference generation: linking words in the generated sentences using pronouns and other kinds of reference</li>
 *     <li>Syntactic and morphological realization: the generation of sentences via a process inverse to parsing, representing the information gathered in the above phases</li>
 *     <li>Phonological or orthographic realization: turning the above into spoken or written words, complete with timing (in the spoken case), punctuation (in the written case), etc.</li>
 * </ol>
 *
 * Created by ceefour on 27/10/2015.
 *
 * @see <a href="http://wiki.opencog.org/w/SegSim">SegSim | OpenCog</a>
 */
@Service
@NaturalLanguage("en")
public class SentenceGenerator {
    private static Logger log = LoggerFactory.getLogger(SentenceGenerator.class);

    @Inject
    protected PronounMapper pronounMapper;
    @Inject @NaturalLanguage("en")
    private IDictionary wordNet;
    @Inject
    protected PreferredMapper preferredMapper;

    /**
     * Generates a clause (not a proper sentence). The first word is not capitalized.
     * @param locale
     * @param expression
     * @return
     */
    public CommunicateAction generate(Locale locale, Proposition expression) {
        Preconditions.checkNotNull(expression, "expression not null");
        final CommunicateAction action = new CommunicateAction();
        String msg = null;
        if (expression instanceof Greeting) {
            Greeting greeting = (Greeting) expression;
            msg = "Good " + greeting.getTimeOfDay();
            if (Pronoun.YOU != greeting.getToPronoun()) {
                // TODO: this is weird
                msg += ", " + pronounMapper.getPronounLabel(Locale.US, greeting.getToPronoun(), PronounCase.OBJECT).get();
            }
        } else if (expression instanceof SpoNoun) {
            final SpoNoun spo = (SpoNoun) expression;
            msg = toText(locale, spo.getSubject(), PronounCase.SUBJECT) + " ";
            final Pronoun pronoun = Optional.ofNullable(spo.getSubject().getPronoun()).orElse(Pronoun.IT);
            msg += toText(locale, spo.getPredicate(), pronoun.getPerson(), pronoun.getNumber()) + " ";
            msg += toText(locale, spo.getObject(), PronounCase.OBJECT);
        } else if (expression instanceof SpoAdj) {
            final SpoAdj spo = (SpoAdj) expression;
            msg = toText(locale, spo.getSubject(), PronounCase.SUBJECT) + " ";
            final Pronoun pronoun = Optional.ofNullable(spo.getSubject().getPronoun()).orElse(Pronoun.IT);
            msg += toText(locale, spo.getPredicate(), pronoun.getPerson(), pronoun.getNumber()) + " ";
            msg += toText(locale, spo.getObject());
        } else if (expression instanceof SpInfinite) {
            final SpInfinite spi = (SpInfinite) expression;
            msg = toText(locale, spi.getSubject(), PronounCase.SUBJECT) + " ";
            final Pronoun pronoun = Optional.ofNullable(spi.getSubject().getPronoun()).orElse(Pronoun.IT);
            msg += toText(locale, spi.getPredicate(), pronoun.getPerson(), pronoun.getNumber());
            if (spi.getToPlace() != null) {
                msg += " to ";
                msg += toText(locale, spi.getToPlace(), PronounCase.OBJECT);
            }
        } else {
            throw new ReasonerException("Unknown proposition class: " + expression.getClass().getName());
        }
        action.setObject(msg);
        return action;
    }

    /**
     * Express noun with its {@link NounArticle}.
     * @param locale
     * @param noun
     * @param pronounCase
     * @return
     */
    public String toText(Locale locale, NounClause noun, PronounCase pronounCase) {
        String result = "";
        if (noun.getArticle() != null) {
            result += noun.getArticle().getEnglish() + " ";
        }
        if (noun.getOwner() != null) {
            result += toText(locale, noun.getOwner(), PronounCase.POSSESSIVE_ADJ) + " ";
        }
        if (noun.getName() != null) {
            result += noun.getName();
            if (PronounCase.POSSESSIVE_ADJ == pronounCase) {
                result += "'s";
            }
        } else if (noun.getPronoun() != null) {
            result += pronounMapper.getPronounLabel(Locale.US, noun.getPronoun(), pronounCase).get();
        } else if (noun.getHref() != null) {
//            result += noun.getHref();
            result += getSynsetLemma(noun.getHref());
            if (PronounCase.POSSESSIVE_ADJ == pronounCase) {
                result += "'s";
            }
        } else {
            throw new ReasonerException("Invalid noun: " + noun);
        }
        return result;
    }

    public String toText(Locale locale, Adjective adj) {
        String result = "";
        if (adj.getHref() != null) {
//            result += adj.getHref();
            result += getSynsetLemma(adj.getHref());
        } else {
            throw new ReasonerException("Invalid adjective: " + adj);
        }
        return result;
    }

    public String toText(Locale locale, Verb verb, PronounPerson person, PronounNumber number) {
        Preconditions.checkNotNull(verb, "Verb is required");
        Preconditions.checkNotNull(verb.getHref(), "Verb %s must have href", verb);
        String result = "";
        if (verb.getModal() != null) {
            result += verb.getModal().getEnglish() + " ";
        }
        if (verb.getHref() != null) {
            result += getSynsetLemma(verb.getHref());
            if (PronounPerson.THIRD == person && PronounNumber.SINGULAR == number) {
                result += "s";
            }
        } else {
            throw new ReasonerException("Invalid verb: " + verb);
        }
        return result;
    }

    public String makeSentence(List<String> clauses, SentenceMood mood) {
        String sentence = clauses.stream().collect(Collectors.joining(", "));
        sentence = StringUtils.capitalize(sentence);
        switch (mood) {
            case STATEMENT: sentence += "."; break;
            case EXCLAMATION: sentence += "!"; break;
            case QUESTION: sentence += "?"; break;
            case DANGLING: sentence += "..."; break;
            case CONFUSED: sentence += "?!?!?!"; break;
            case HYPERBOLIC: sentence += "!!!!!!"; break;
            default:
                throw new ReasonerException("Unknown sentence mood: " + mood);
        }
        return sentence;
    }

    /**
     * The first digit of the synset identifies the part-of-speech according to the following table
     Part-of-speech	Letter code	Numeric code
     Noun	n	1
     Verb	v	2
     Adjective	a	3
     Adverb	r	4
     Adjective Satellite	s	3
     Phrase	p	4
     * @param href Format must be either "yago:wordnet_[word]_[synsetId]" or "wn30:[8digit]-[pos]".
     *             Synset ID in WordNet 3.1 format (but we're still using WordNet 3.0 data).
     *             Example: "yago:wordnet_entity_100001740".
     * @return
     */
    public ISynsetID hrefToSynsetId(String href) {
        final String nsPrefix = StringUtils.substringBefore(href, ":");
        final String synsetId;
        if ("yago".equals(nsPrefix)) {
            final String digits9 = StringUtils.substringAfterLast(href, "_");
            final char numeric = digits9.charAt(0);
            final char pos;
            switch (numeric) {
                case '1':
                    pos = 'n';
                    break;
                case '2':
                    pos = 'v';
                    break;
                case '3':
                    pos = 'a';
                    break; // TODO: can be 'a' or 's' !
                case '4':
                    pos = 'r';
                    break; // TODO: can be 'r' or 'p' !
                default:
                    throw new ReasonerException("Unknown WordNet QName: " + href);
            }
            synsetId = "SID-" + digits9.substring(1, digits9.length()) + "-" + Character.toUpperCase(pos);
        } else if ("wn30".equals(nsPrefix)) {
            synsetId = "SID-" + StringUtils.substringAfter(href, ":").toUpperCase();
        } else {
            throw new ReasonerException("Unknown nsPrefix: " + nsPrefix);
        }
        try {
            return SynsetID.parseSynsetID(synsetId);
        } catch (Exception e) {
            throw new ReasonerException(e, "Cannot parse synset '%s'", synsetId);
        }
    }

    private ISynset getSynset(String href) {
        return wordNet.getSynset(hrefToSynsetId(href));
    }

    protected String getSynsetLemma(String href) {
        // return preferred lemma if exists
        final Optional<String> prefLemma = preferredMapper.getPreferred(href, Locale.US);
        if (prefLemma.isPresent()) {
            return prefLemma.get();
        }
        // otherwise consult WordNet
        final ISynsetID synsetId = hrefToSynsetId(href);
        final ISynset synset = Preconditions.checkNotNull(wordNet.getSynset(synsetId),
                "href %s synset %s not found, probably synset ID mismatch due to different WordNet version", href, synsetId);
        Preconditions.checkState(!synset.getWords().isEmpty(),
                "No words for href %s synset %s", href, synset);
        final IWord word = synset.getWords().iterator().next();
        return word.getLemma();
    }

}

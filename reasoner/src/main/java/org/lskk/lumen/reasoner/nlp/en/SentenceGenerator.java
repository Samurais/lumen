package org.lskk.lumen.reasoner.nlp.en;

import com.google.common.base.Preconditions;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.expression.Greeting;
import org.lskk.lumen.reasoner.nlp.Pronoun;
import org.lskk.lumen.reasoner.expression.SpoAdj;
import org.lskk.lumen.reasoner.expression.SpoNoun;
import org.lskk.lumen.reasoner.nlp.NaturalLanguage;
import org.lskk.lumen.reasoner.nlp.NounClause;
import org.lskk.lumen.reasoner.nlp.PronounMapper;
import org.lskk.lumen.reasoner.nlp.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Locale;

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
    private PronounMapper pronounMapper;

    /**
     * Generates a clause (not a proper sentence). The first word is not capitalized.
     * @param locale
     * @param expression
     * @return
     */
    public CommunicateAction generate(Locale locale, Object expression) {
        Preconditions.checkNotNull(expression, "expression not null");
        final CommunicateAction action = new CommunicateAction();
        String msg = null;
        if (expression instanceof Greeting) {
            Greeting greeting = (Greeting) expression;
            msg = "Good " + greeting.getTimeOfDay();
            if (Pronoun.YOU != greeting.getToPronoun()) {
                msg += ", " + pronounMapper.getPronounLabel(Locale.US, greeting.getToPronoun()).get();
            }
        } else if (expression instanceof SpoNoun) {
            final SpoNoun spo = (SpoNoun) expression;
            msg = toText(locale, spo.getSubject()) + " ";
            msg += toText(locale, spo.getPredicate()) + " ";
            msg += toText(locale, spo.getObject());
        } else if (expression instanceof SpoAdj) {

        } else {
            log.warn("Unknown expression class: {}", expression.getClass().getName());
        }
        action.setObject(msg);
        return action;
    }

    public String toText(Locale locale, NounClause noun) {
        String result = "";
        if (noun.getOwner() != null) {
            result += toText(locale, noun.getOwner()) + "'s ";
        }
        if (noun.getName() != null) {
            result += noun.getName();
        } else if (noun.getPronoun() != null) {
            result += pronounMapper.getPronounLabel(Locale.US, noun.getPronoun()).get();
        } else if (noun.getHref() != null) {
            // FIXME: yago entity to text
            result += noun.getHref();
        } else {
            throw new ReasonerException("Invalid noun: " + noun);
        }
        return result;
    }

    public String toText(Locale locale, Verb verb) {
        String result = "";
        if (verb.getHref() != null) {
            // FIXME: yago entity to text
            result += verb.getHref();
        } else {
            throw new ReasonerException("Invalid verb: " + verb);
        }
        return result;
    }

}

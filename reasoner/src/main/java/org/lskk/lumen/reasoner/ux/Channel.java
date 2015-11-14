package org.lskk.lumen.reasoner.ux;

import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.expression.Proposition;
import org.lskk.lumen.reasoner.nlp.NaturalLanguage;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.TwitterException;

import javax.inject.Inject;
import java.util.Locale;

/**
 * Provides a way to interact with person, either physically or via social media.
 * A channel has a default {@link Locale}.
 * Created by ceefour on 14/11/2015.
 */
public abstract class Channel {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected Locale inLanguage = Locale.US;
    @Inject @NaturalLanguage("en")
    protected SentenceGenerator sentenceGenerator_en;
    @Inject @NaturalLanguage("id")
    protected SentenceGenerator sentenceGenerator_id;

    public Channel() {
    }

    public Channel(SentenceGenerator sentenceGenerator_en, SentenceGenerator sentenceGenerator_id) {
        this.sentenceGenerator_en = sentenceGenerator_en;
        this.sentenceGenerator_id = sentenceGenerator_id;
    }

    public Locale getInLanguage() {
        return inLanguage;
    }

    /**
     * {@link org.lskk.lumen.reasoner.aiml.AimlService} will call this after it
     * knows the incoming language.
     * @param inLanguage
     */
    public void setInLanguage(Locale inLanguage) {
        this.inLanguage = inLanguage;
    }

    public abstract void express(CommunicateAction communicateAction);

    public void express(Proposition proposition) {
        final CommunicateAction action;
        switch (inLanguage.getLanguage()) {
            case "en":
                action = sentenceGenerator_en.generate(inLanguage, proposition);
                break;
            case "in":
                action = sentenceGenerator_id.generate(inLanguage, proposition);
                break;
            default:
                throw new ReasonerException("Unhandled locale: " + inLanguage.toLanguageTag());
        }
        action.setImage(proposition.getImage());
        express(action);
    }

}

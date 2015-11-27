package org.lskk.lumen.reasoner.ux;

import org.apache.commons.beanutils.BeanUtils;
import org.lskk.lumen.core.AudioObject;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.core.ImageObject;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.expression.Proposition;
import org.lskk.lumen.reasoner.nlp.NaturalLanguage;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * Provides a way to interact with person, either physically or via social media.
 * A channel has a default {@link Locale}.
 * Created by ceefour on 14/11/2015.
 */
public abstract class Channel<P> {

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

    /**
     * @param avatarId Avatar that expresses this {@link CommunicateAction}.
     * @param communicateAction
     * @param params
     */
    public abstract void express(String avatarId, CommunicateAction communicateAction, P params);

    /**
     * @param avatarId Avatar that expresses this {@link Proposition}.
     * @param proposition
     * @param usedForSynthesis
     * @param params
     */
    public void express(String avatarId, Proposition proposition, boolean usedForSynthesis, P params) {
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
        action.setUsedForSynthesis(usedForSynthesis);
        try {
            if (proposition.getImage() != null) {
                action.setImage((ImageObject) BeanUtils.cloneBean(proposition.getImage()));
            }
            if (proposition.getAudio() != null) {
                action.setAudio((AudioObject) BeanUtils.cloneBean(proposition.getAudio()));
            }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new ReasonerException(e, "Cannot clone %s", proposition.getImage());
        }
        express(avatarId, action, params);
    }

}

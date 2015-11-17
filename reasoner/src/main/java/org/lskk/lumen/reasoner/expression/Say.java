package org.lskk.lumen.reasoner.expression;

import java.io.Serializable;

/**
 * Created by ceefour on 29/10/2015.
 * @see <a href="http://www.wagsoft.com/Papers/Thesis/11Generation.pdf">WagSoft Sentence Generation</a>
 */
public class Say implements Serializable {
    private SpeechFunction speechFunction = SpeechFunction.STATEMENT;
    private Tense tense = Tense.SIMPLE_PRESENT;
    private Polarity polarity = Polarity.AFFIRMATIVE;
    private Modality modality = Modality.NONE;
    private Speaker speaker;
    private Speaker hearer;
    private Proposition proposition;

    public SpeechFunction getSpeechFunction() {
        return speechFunction;
    }

    public void setSpeechFunction(SpeechFunction speechFunction) {
        this.speechFunction = speechFunction;
    }

    public Tense getTense() {
        return tense;
    }

    public void setTense(Tense tense) {
        this.tense = tense;
    }

    public Polarity getPolarity() {
        return polarity;
    }

    public void setPolarity(Polarity polarity) {
        this.polarity = polarity;
    }

    public Modality getModality() {
        return modality;
    }

    public void setModality(Modality modality) {
        this.modality = modality;
    }

    public Speaker getSpeaker() {
        return speaker;
    }

    public void setSpeaker(Speaker speaker) {
        this.speaker = speaker;
    }

    public Speaker getHearer() {
        return hearer;
    }

    public void setHearer(Speaker hearer) {
        this.hearer = hearer;
    }

    public Proposition getProposition() {
        return proposition;
    }

    public void setProposition(Proposition proposition) {
        this.proposition = proposition;
    }

    @Override
    public String toString() {
        return "Say{" +
                "speechFunction=" + speechFunction +
                ", tense=" + tense +
                ", polarity=" + polarity +
                ", modality=" + modality +
                ", speaker=" + speaker +
                ", proposition=" + proposition +
                '}';
    }
}

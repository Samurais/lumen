package org.lskk.lumen.reasoner.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.ux.Channel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by ceefour on 26/10/2015.
 */
public class AgentResponse implements Serializable {

    private List<Object> stimuli = new ArrayList<>();
    private Locale stimuliLanguage;
    private float[] matchingTruthValue;
    private List<CommunicateAction> communicateActions = new ArrayList<>();
    private UnrecognizedInput unrecognizedInput;
    private List<Serializable> insertables = new ArrayList<>();

    public AgentResponse(Object stimuli) {
        this.stimuli.add(stimuli);
    }

    public AgentResponse(Object stimuli, CommunicateAction communicateAction) {
        this.stimuli.add(stimuli);
        this.communicateActions.add(communicateAction);
    }

    public AgentResponse(List<Object> stimuli, CommunicateAction communicateAction) {
        this.stimuli.addAll(stimuli);
        this.communicateActions.add(communicateAction);
    }

    public AgentResponse(List<Object> stimuli, UnrecognizedInput unrecognizedInput) {
        this.stimuli.addAll(stimuli);
        this.unrecognizedInput = unrecognizedInput;
    }

    public AgentResponse(Object stimuli, UnrecognizedInput unrecognizedInput) {
        this.stimuli.add(stimuli);
        this.unrecognizedInput = unrecognizedInput;
    }

    public List<Object> getStimuli() {
        return stimuli;
    }

    /**
     * Language of {@link #getStimuli()} as determined by {@link org.lskk.lumen.reasoner.aiml.AimlService#process(Locale, String, Channel, String, boolean)}.
     * @return
     */
    public Locale getStimuliLanguage() {
        return stimuliLanguage;
    }

    @JsonGetter("stimuliLanguage")
    public String getStimuliLanguageAsString() {
        return stimuliLanguage != null ? stimuliLanguage.toLanguageTag() : null;
    }

    public void setStimuliLanguage(Locale stimuliLanguage) {
        this.stimuliLanguage = stimuliLanguage;
    }

    @JsonSetter
    public void setStimuliLanguage(String stimuliLanguage) {
        this.stimuliLanguage = stimuliLanguage != null ? Locale.forLanguageTag(stimuliLanguage) : null;
    }
    /**
     * Truth value of the matching category given received input.
     * @return
     */
    public float[] getMatchingTruthValue() {
        return matchingTruthValue;
    }

    public void setMatchingTruthValue(float[] matchingTruthValue) {
        this.matchingTruthValue = matchingTruthValue;
    }

    public List<CommunicateAction> getCommunicateActions() {
        return communicateActions;
    }

    /**
     * Events to be inserted.
     * @return
     */
    public List<Serializable> getInsertables() {
        return insertables;
    }

    public UnrecognizedInput getUnrecognizedInput() {
        return unrecognizedInput;
    }

    @Override
    public String toString() {
        return "AgentResponse{" +
                "stimuli=" + stimuli +
                ", communicateActions=" + communicateActions +
                ", insertables=" + insertables +
                '}';
    }
}

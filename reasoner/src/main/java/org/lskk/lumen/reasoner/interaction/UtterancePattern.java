package org.lskk.lumen.reasoner.interaction;

import org.lskk.lumen.core.ConversationStyle;
import org.lskk.lumen.core.IConfidenceAware;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by ceefour on 25/02/2016.
 * @see LocalizedString
 */
public class UtterancePattern implements Serializable, IConfidenceAware {

    public enum Scope {
        /**
         * Other Task is active, but this utterance can trigger activation of this PromptTask.
         */
        GLOBAL,
        /**
         * Local scope that is only active when the PromptTask is directly accessed.
         */
        LOCAL,
        /**
         * Used only for matching: Matches either {@link #GLOBAL} or {@link #LOCAL}.
         */
        ANY
    }

    private String inLanguage;
    private String pattern;
    private Scope scope = Scope.GLOBAL;
    private ConversationStyle style;
    private Float confidence;
    private String actual;
    private Map<String, String> slotStrings = new LinkedHashMap<>();
    private Map<String, Object> slotValues = new LinkedHashMap<>();

    /**
     * If {@code null}, this pattern matches any {@link java.util.Locale}, usually useful for {@link Scope#LOCAL}
     * utterances.
     * @return
     */
    public String getInLanguage() {
        return inLanguage;
    }

    public void setInLanguage(String inLanguage) {
        this.inLanguage = inLanguage;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    /**
     * {@code null} means neutral.
     * @return
     */
    public ConversationStyle getStyle() {
        return style;
    }

    public void setStyle(ConversationStyle style) {
        this.style = style;
    }

    /**
     * Only used during match: confidence of the match.
     * @return
     */
    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    /**
     * Only used during match: actual utterance.
     * @return
     */
    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    /**
     * Only used during match: captured slot raw strings.
     * @return
     */
    public Map<String, String> getSlotStrings() {
        return slotStrings;
    }

    /**
     * Only used during match: captured slot values (converted to target type).
     * @return
     */
    public Map<String, Object> getSlotValues() {
        return slotValues;
    }

    @Override
    public String toString() {
        return "UtterancePattern{" +
                "inLanguage='" + inLanguage + '\'' +
                ", pattern='" + pattern + '\'' +
                ", scope=" + scope +
                ", confidence=" + confidence +
                ", actual='" + actual + '\'' +
                ", slotStrings=" + slotStrings +
                ", slotValues=" + slotValues +
                '}';
    }
}

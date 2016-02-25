package org.lskk.lumen.reasoner.interaction;

import org.lskk.lumen.core.ConversationStyle;

import java.io.Serializable;

/**
 * Created by ceefour on 24/02/2016.
 * @see LocalizedString
 */
public class QuestionTemplate implements Serializable {

    private String inLanguage;
    private String object;
    private ConversationStyle style;

    public String getInLanguage() {
        return inLanguage;
    }

    public void setInLanguage(String inLanguage) {
        this.inLanguage = inLanguage;
    }

    /**
     * It can be SSML or a pattern string.
     * @return
     */
    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
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
}

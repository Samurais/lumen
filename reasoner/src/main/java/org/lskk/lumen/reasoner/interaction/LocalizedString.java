package org.lskk.lumen.reasoner.interaction;

import java.io.Serializable;

/**
 * Created by ceefour on 24/02/2016.
 */
public class LocalizedString implements Serializable {
    private String inLanguage;
    private String object;

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
}

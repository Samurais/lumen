package org.lskk.lumen.reasoner.nlp;

import java.io.Serializable;

/**
 * i.e. happy, angry, sad, etc.
 * Created by ceefour on 27/10/2015.
 */
public class Adjective implements Serializable {
    private String href;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}

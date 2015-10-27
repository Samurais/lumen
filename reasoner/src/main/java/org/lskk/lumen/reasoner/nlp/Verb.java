package org.lskk.lumen.reasoner.nlp;

import java.io.Serializable;

/**
 * Created by ceefour on 27/10/2015.
 */
public class Verb implements Serializable {
    private String href;

    public Verb() {
    }

    public Verb(String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}

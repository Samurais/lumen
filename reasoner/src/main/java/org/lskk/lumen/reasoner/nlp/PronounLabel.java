package org.lskk.lumen.reasoner.nlp;

import java.io.Serializable;

/**
 * Created by ceefour on 27/10/2015.
 */
public class PronounLabel implements Serializable {
    private String subject;
    private String object;
    private String possessiveAdj;
    private String possessivePronoun;
    private String reflexive;

    public String getSubject() {
        return subject;
    }

    public String getObject() {
        return object;
    }

    public String getPossessiveAdj() {
        return possessiveAdj;
    }

    public String getPossessivePronoun() {
        return possessivePronoun;
    }

    public String getReflexive() {
        return reflexive;
    }
}

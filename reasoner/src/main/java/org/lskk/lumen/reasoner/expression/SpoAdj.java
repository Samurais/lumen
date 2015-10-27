package org.lskk.lumen.reasoner.expression;

import org.lskk.lumen.reasoner.nlp.Adjective;
import org.lskk.lumen.reasoner.nlp.NounClause;
import org.lskk.lumen.reasoner.nlp.Verb;

import java.io.Serializable;

/**
 * The object must be adjective.
 * e.g. "My mood is sad", "I am happy".
 * Created by ceefour on 10/2/15.
 */
public class SpoAdj implements Serializable {
    private NounClause subject;
    private Verb predicate;
    private Adjective object;

    public SpoAdj() {
    }

    public SpoAdj(NounClause subject, Verb predicate, Adjective object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public NounClause getSubject() {
        return subject;
    }

    public void setSubject(NounClause subject) {
        this.subject = subject;
    }

    public Verb getPredicate() {
        return predicate;
    }

    public void setPredicate(Verb predicate) {
        this.predicate = predicate;
    }

    public Adjective getObject() {
        return object;
    }

    public void setObject(Adjective object) {
        this.object = object;
    }
}
package org.lskk.lumen.reasoner.expression;

import org.lskk.lumen.reasoner.nlp.Adjective;
import org.lskk.lumen.reasoner.nlp.NounClause;
import org.lskk.lumen.reasoner.nlp.Verb;

import java.io.Serializable;

/**
 * The object must be noun.
 * e.g. "My name is Hendy", Your girlfriend is Neni.
 * Created by ceefour on 10/2/15.
 */
public class SpoNoun implements Serializable {
    private NounClause subject;
    private Verb predicate;
    private NounClause object;

    public SpoNoun() {
    }

    public SpoNoun(NounClause subject, Verb predicate, NounClause object) {
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

    public NounClause getObject() {
        return object;
    }

    public void setObject(NounClause object) {
        this.object = object;
    }
}
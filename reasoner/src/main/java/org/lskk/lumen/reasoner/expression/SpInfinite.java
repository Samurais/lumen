package org.lskk.lumen.reasoner.expression;

import org.lskk.lumen.reasoner.nlp.NounClause;
import org.lskk.lumen.reasoner.nlp.Verb;

/**
 * e.g. "I should go to the zoo."
 * Created by ceefour on 10/2/15.
 */
public class SpInfinite extends Proposition {
    private NounClause subject;
    private Verb predicate;
    private NounClause toPlace;

    public SpInfinite() {
    }

    public SpInfinite(NounClause subject, Verb predicatet) {
        this.subject = subject;
        this.predicate = predicate;
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

    public NounClause getToPlace() {
        return toPlace;
    }

    public void setToPlace(NounClause toPlace) {
        this.toPlace = toPlace;
    }

    @Override
    public String toString() {
        return "SpInfinite{" +
                "subject=" + subject +
                ", predicate=" + predicate +
                ", toPlace=" + toPlace +
                '}';
    }
}
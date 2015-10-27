package org.lskk.lumen.reasoner.expression;

import org.lskk.lumen.reasoner.nlp.Pronoun;
import org.lskk.lumen.reasoner.event.TimeOfDay;

import java.io.Serializable;

/**
 * Created by ceefour on 10/2/15.
 */
public class Greeting implements Serializable {
    private String fromName;
    private TimeOfDay timeOfDay;
    private Pronoun toPronoun;

    public TimeOfDay getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(TimeOfDay timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public Pronoun getToPronoun() {
        return toPronoun;
    }

    public void setToPronoun(Pronoun toPronoun) {
        this.toPronoun = toPronoun;
    }
}

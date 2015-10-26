package org.lskk.lumen.reasoner.event;

import java.io.Serializable;

/**
 * Created by ceefour on 10/2/15.
 */
public class Greeting implements Serializable {
    private String fromName;
    private TimeOfDay timeOfDay;
    private String toPronoun;

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

    public String getToPronoun() {
        return toPronoun;
    }

    public void setToPronoun(String toPronoun) {
        this.toPronoun = toPronoun;
    }
}

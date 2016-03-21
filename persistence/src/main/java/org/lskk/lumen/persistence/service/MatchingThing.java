package org.lskk.lumen.persistence.service;

import org.lskk.lumen.persistence.neo4j.Thing;

import java.io.Serializable;

/**
 * Created by ceefour on 14/02/2016.
 */
public class MatchingThing implements Serializable, Comparable<MatchingThing> {
    private Thing thing;
    private float confidence;

    public MatchingThing() {
    }

    public MatchingThing(Thing thing, float confidence) {
        this.thing = thing;
        this.confidence = confidence;
    }

    public Thing getThing() {
        return thing;
    }

    public void setThing(Thing thing) {
        this.thing = thing;
    }

    /**
     * Confidence part (non-normalized) of the truth value.
     * @return
     */
    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    @Override
    public int compareTo(MatchingThing o) {
        return (int) Math.signum(o.getConfidence() - getConfidence());
    }
}

package org.lskk.lumen.persistence.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 22/02/2016.
 */
public class MatchingThings implements Serializable {
    public List<MatchingThing> matches = new ArrayList<>();

    public MatchingThings() {
    }

    public MatchingThings(List<MatchingThing> matches) {
        this.matches = matches;
    }

    public List<MatchingThing> getMatches() {
        return matches;
    }
}

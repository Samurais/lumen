package org.lskk.lumen.reasoner;

import org.lskk.lumen.reasoner.expression.Proposition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Child stories.
 * Created by ceefour on 07/11/2015.
 */
public class Story implements Serializable {
    private List<Proposition> propositions = new ArrayList<>();

    public List<Proposition> getPropositions() {
        return propositions;
    }
}

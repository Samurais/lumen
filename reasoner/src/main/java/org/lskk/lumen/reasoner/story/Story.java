package org.lskk.lumen.reasoner.story;

import org.lskk.lumen.reasoner.expression.Proposition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Child stories.
 * Created by ceefour on 07/11/2015.
 */
public class Story implements Serializable {
    private String id;
    private String name;
    private List<Proposition> propositions = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Proposition> getPropositions() {
        return propositions;
    }

    @Override
    public String toString() {
        return "Story{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", propositions=" + propositions.size() +
                '}';
    }
}

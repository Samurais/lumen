package org.lskk.lumen.reasoner.goal;

import org.lskk.lumen.reasoner.story.TellStory;

import java.io.Serializable;

/**
 * Created by ceefour on 12/11/2015.
 */
public class PropositionTold implements Serializable {
    private TellStory tellStory;
    private int index;

    public PropositionTold(TellStory tellStory, int index) {
        this.tellStory = tellStory;
        this.index = index;
    }

    public TellStory getTellStory() {
        return tellStory;
    }

    public int getIndex() {
        return index;
    }
}

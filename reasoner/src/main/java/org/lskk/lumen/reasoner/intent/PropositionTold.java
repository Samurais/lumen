package org.lskk.lumen.reasoner.intent;

import org.lskk.lumen.reasoner.story.TellStory;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by ceefour on 12/11/2015.
 */
public class PropositionTold implements Serializable {
    private UUID id = UUID.randomUUID();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropositionTold that = (PropositionTold) o;

        if (index != that.index) return false;
        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + index;
        return result;
    }
}

package org.lskk.lumen.reasoner.goal;

import org.lskk.lumen.reasoner.story.Story;

import java.util.UUID;

/**
 * Goal that tells a story to a person.
 * Created by ceefour on 07/11/2015.
 */
public class TellStory extends Goal {
    private UUID id = UUID.randomUUID();
    private String storyId;
    private Story story;
    private Integer lastPropositionIndex;
    private boolean done;

    public String getStoryId() {
        return storyId;
    }

    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }

    public Story getStory() {
        return story;
    }

    public void setStory(Story story) {
        this.story = story;
    }

    /**
     * Last proposition that was uttered.
     * @return
     */
    public Integer getLastPropositionIndex() {
        return lastPropositionIndex;
    }

    public void setLastPropositionIndex(Integer lastPropositionIndex) {
        this.lastPropositionIndex = lastPropositionIndex;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    @Override
    public String toString() {
        return "TellStory{" +
                "storyId='" + storyId + '\'' +
                ", story=" + story +
                ", lastPropositionIndex=" + lastPropositionIndex +
                ", done=" + done +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TellStory tellStory = (TellStory) o;

        if (done != tellStory.done) return false;
        if (!id.equals(tellStory.id)) return false;
        if (storyId != null ? !storyId.equals(tellStory.storyId) : tellStory.storyId != null) return false;
        return !(lastPropositionIndex != null ? !lastPropositionIndex.equals(tellStory.lastPropositionIndex) : tellStory.lastPropositionIndex != null);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (storyId != null ? storyId.hashCode() : 0);
        result = 31 * result + (lastPropositionIndex != null ? lastPropositionIndex.hashCode() : 0);
        result = 31 * result + (done ? 1 : 0);
        return result;
    }
}

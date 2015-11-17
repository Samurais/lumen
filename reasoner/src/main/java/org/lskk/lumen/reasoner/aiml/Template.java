package org.lskk.lumen.reasoner.aiml;

import org.lskk.lumen.core.AudioObject;
import org.lskk.lumen.core.ImageObject;
import org.lskk.lumen.reasoner.goal.Goal;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 10/28/15.
 */
public class Template implements Serializable {
    private String srai;
    private Sr sr;
    private List<Serializable> contents = new ArrayList<>();
    private List<Choice> randoms = new ArrayList<>();
    private ImageObject image;
    private AudioObject audio;
    private List<GoalElement> goals = new ArrayList<>();

    /**
     * Symbolic reduction artificial intelligence.
     * i.e. redirect to another {@link Category}, using the srai as input text.
     */
    @XmlElement(name="srai")
    public String getSrai() {
        return srai;
    }

    public void setSrai(String srai) {
        this.srai = srai;
    }

    public boolean hasBodies() {
        return contents != null && !contents.isEmpty();
    }

    /**
     * May contain "get", e.g.
     *
     * Hi there. I was just wanting to talk to &lt;get name="name"/>.
     */
    @XmlMixed
    @XmlElementRef(name = "get", type = Get.class)
    public List<Serializable> getContents() {
        return contents;
    }

    @XmlTransient
    public String getContentsString() {
        return contents != null ? contents.stream().map(Object::toString).collect(Collectors.joining(" ")).trim() : "";
    }

    @XmlElement(name="sr")
    public Sr getSr() {
        return sr;
    }

    @XmlElementWrapper(name = "random") @XmlElement(name="li")
    public List<Choice> getRandoms() {
        return randoms;
    }

//    public void setRandoms(List<Choice> randoms) {
//        this.randoms = randoms;
//    }

    @XmlElement(name="image")
    public ImageObject getImage() {
        return image;
    }

    public void setImage(ImageObject image) {
        this.image = image;
    }

    @XmlElement(name="audio")
    public AudioObject getAudio() {
        return audio;
    }

    public void setAudio(AudioObject audio) {
        this.audio = audio;
    }

    @XmlElement(name = "goal")
    public List<GoalElement> getGoals() {
        return goals;
    }

    @Override
    public String toString() {
        return "Template{" +
                "srai=" + srai +
                ", contents=" + getContentsString() +
                ", randoms=" + randoms +
                ", image=" + image +
                '}';
    }
}

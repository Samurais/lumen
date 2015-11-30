package org.lskk.lumen.reasoner.aiml;

import com.google.common.collect.ImmutableList;
import org.lskk.lumen.core.AudioObject;
import org.lskk.lumen.core.EmotionKind;
import org.lskk.lumen.core.ImageObject;
import org.lskk.lumen.reasoner.goal.Goal;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 10/28/15.
 */
public class Template implements Serializable {
    private String srai;
    private Sr sr;
    private Locale lang;
    private Locale ifLang;
    private Boolean synthesis;
    private EmotionKind emotion;
    //private SayElement defaultSay;
//    private List<Serializable> contents = new ArrayList<>();
    private List<Choice> randoms = new ArrayList<>();
//    private ImageObject image;
//    private AudioObject audio;
    private List<SayElement> says = new ArrayList<>();
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
        return says.stream().findFirst().map(it -> !it.getContents().isEmpty()).orElse(false);
    }
//    public boolean hasBodies() {
//        return contents.isEmpty();
//    }

    /**
     * May contain "get", e.g.
     *
     * Hi there. I was just wanting to talk to &lt;get name="name"/>.
     * @deprecated use {@link SayElement#getContents()}.
     */
    @Deprecated
    public List<Serializable> getContents() {
        return says.stream().findFirst().map(SayElement::getContents).orElse(null);
//        return contents;
    }

    @XmlMixed
    @XmlElementRef(name = "get", type = Get.class)
    public void setContents(List<Serializable> contents) {
        if (says.isEmpty()) {
            says.add(new SayElement());
        }
        says.get(0).getContents().addAll(contents);
    }

    /**
     * @deprecated Use {@link SayElement#getContentsString()}.
     * @return
     */
    @XmlTransient
    @Deprecated
    public String getContentsString() {
//        return !contents.isEmpty() ? contents.stream().map(Object::toString).collect(Collectors.joining(" ")).trim() : "";
        return says.stream().findFirst().map(SayElement::getContentsString).orElse("");
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

//    @Deprecated
//    @XmlElement
//    public ImageObject getImage() {
//        return says.stream().findFirst().map(SayElement::getImage).orElse(null);
//    }
//
//    public void setImage(ImageObject image) {
//        if (says.isEmpty()) {
//            says.add(new SayElement());
//        }
//        says.get(0).setImage(image);
//    }
//
//    @Deprecated
//    @XmlElement
//    public AudioObject getAudio() {
//        return says.stream().findFirst().map(SayElement::getAudio).orElse(null);
//    }
//
//    public void setAudio(AudioObject audio) {
//        if (says.isEmpty()) {
//            says.add(new SayElement());
//        }
//        says.get(0).setAudio(audio);
//    }

    /**
     * Warning: do not mix default Say (using {@link #getContents()}) with
     * dedicated {@link SayElement}(s).
     * @return
     */
    @XmlElement(name = "say")
    public List<SayElement> getSays() {
        return says;
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
                '}';
    }
}

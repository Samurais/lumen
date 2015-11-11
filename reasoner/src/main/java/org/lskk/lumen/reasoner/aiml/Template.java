package org.lskk.lumen.reasoner.aiml;

import org.lskk.lumen.core.ImageObject;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 10/28/15.
 */
public class Template implements Serializable {
    /**
     * Symbolic reduction artificial intelligence.
     * i.e. redirect to another {@link Category}, using the srai as input text.
     */
    @XmlElement(name="srai")
    private String srai;
    @XmlElement(name="sr")
    private Sr sr;
    /**
     * May contain "get", e.g.
     *
     * Hi there. I was just wanting to talk to &lt;get name="name"/>.
     */
    @XmlMixed
    @XmlElementRef(name = "get", type = Get.class)
    private List<Serializable> contents;
    @XmlElementWrapper(name = "random") @XmlElement(name="li")
    private List<Choice> randoms;
    @XmlElement(name="image")
    private ImageObject image;

    @XmlTransient
    public String getSrai() {
        return srai;
    }

    public void setSrai(String srai) {
        this.srai = srai;
    }

    public boolean hasBodies() {
        return contents != null && !contents.isEmpty();
    }

    @XmlTransient
    public List<Serializable> getContents() {
        return contents;
    }

    @XmlTransient
    public String getContentsString() {
        return contents != null ? contents.stream().map(Object::toString).collect(Collectors.joining(" ")).trim() : "";
    }

    public Sr getSr() {
        return sr;
    }

    @XmlTransient
    public List<Choice> getRandoms() {
        return randoms;
    }

//    public void setRandoms(List<Choice> randoms) {
//        this.randoms = randoms;
//    }

    @XmlTransient
    public ImageObject getImage() {
        return image;
    }

    public void setImage(ImageObject image) {
        this.image = image;
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

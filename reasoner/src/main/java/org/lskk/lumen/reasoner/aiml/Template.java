package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 10/28/15.
 */
public class Template implements Serializable {
    @XmlElement(name="srai")
    private Srai srai;
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
    private List<String> randoms;

    @XmlTransient
    public Srai getSrai() {
        return srai;
    }

    public void setSrai(Srai srai) {
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

    @Override
    public String toString() {
        return "Template{" +
                "srai=" + srai +
                ", contents=" + getContentsString() +
                ", randoms=" + randoms +
                '}';
    }
}

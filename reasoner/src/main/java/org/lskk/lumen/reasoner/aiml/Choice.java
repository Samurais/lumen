package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 29/10/2015.
 */
public class Choice implements Serializable {
    @XmlElement(name="srai")
    private String srai;
    @XmlMixed
    private List<Serializable> contents;

    @XmlTransient
    public String getSrai() {
        return srai;
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
        return "Choice{" +
                "srai='" + srai + '\'' +
                ", contents=" + contents +
                '}';
    }
}

package org.lskk.lumen.reasoner.ux;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlMixed;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 21/02/2016.
 */
public class FragmentElement {

    private String id;
    private List<Serializable> contents = new ArrayList<>();

    @XmlAttribute()
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlMixed
    @XmlElementRef(name = "span", type = SpanElement.class)
    public List<Serializable> getContents() {
        return contents;
    }
}

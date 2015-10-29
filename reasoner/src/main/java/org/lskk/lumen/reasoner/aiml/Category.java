package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

/**
 * Created by ceefour on 10/28/15.
 */
public class Category implements Serializable {
    @XmlElement
    private String pattern;
    @XmlElement
    private Template template;

    @XmlTransient
    public String getPattern() {
        return pattern;
    }

    @XmlTransient
    public Template getTemplate() {
        return template;
    }

    @Override
    public String toString() {
        return "Category{" +
                "pattern='" + pattern + '\'' +
                ", template=" + template +
                '}';
    }
}

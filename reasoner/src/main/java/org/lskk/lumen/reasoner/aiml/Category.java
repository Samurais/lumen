package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import java.io.Serializable;

/**
 * Created by ceefour on 10/28/15.
 */
public class Category implements Serializable {
    @XmlElement
    private String pattern;
    @XmlElement
    private Template template;

    @Override
    public String toString() {
        return "Category{" +
                "pattern='" + pattern + '\'' +
                ", template=" + template +
                '}';
    }
}

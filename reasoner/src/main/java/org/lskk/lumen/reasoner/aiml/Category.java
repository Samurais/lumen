package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 10/28/15.
 */
public class Category implements Serializable {
    private List<Pattern> patterns = new ArrayList<>();
    private Template template;

    @XmlElement(name = "pattern")
    public List<Pattern> getPatterns() {
        return patterns;
    }

    @XmlElement(name = "template")
    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    @Override
    public String toString() {
        return "Category{" +
                "patterns='" + patterns + '\'' +
                ", template=" + template +
                '}';
    }
}

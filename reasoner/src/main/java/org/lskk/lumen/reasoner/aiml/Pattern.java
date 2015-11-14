package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;
import java.util.Locale;

/**
 * Created by ceefour on 14/11/2015.
 */
public class Pattern implements Serializable {

    private String content;
    private String inLanguage;

    @XmlValue
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @XmlAttribute(name = "lang")
    public String getInLanguage() {
        return inLanguage;
    }

    public void setInLanguage(String inLanguage) {
        this.inLanguage = inLanguage;
    }

    @Override
    public String toString() {
        return inLanguage + ':'+ content;
    }
}

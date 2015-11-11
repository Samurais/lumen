package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 10/28/15.
 */
@XmlRootElement(name = "aiml")
public class Aiml implements Serializable {
    private String version;
    @XmlElement(name="category")
    private List<Category> categories = new ArrayList<>();

    public List<Category> getCategories() {
        return categories;
    }

    public String getVersion() {
        return version;
    }

    @XmlAttribute
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Aiml{" +
                "categories=" + categories +
                '}';
    }
}

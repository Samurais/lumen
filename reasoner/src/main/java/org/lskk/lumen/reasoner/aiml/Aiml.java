package org.lskk.lumen.reasoner.aiml;

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
    @XmlElement(name="category")
    private List<Category> categories = new ArrayList<>();

    public List<Category> getCategories() {
        return categories;
    }

    @Override
    public String toString() {
        return "Aiml{" +
                "categories=" + categories +
                '}';
    }
}

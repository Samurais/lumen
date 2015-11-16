package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 07/11/2015.
 */
public class GoalElement implements Serializable {
    private String clazz;
    private List<GoalProperty> properties = new ArrayList<>();

    @XmlAttribute(name = "class")
    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    @XmlElement(name = "property")
    public List<GoalProperty> getProperties() {
        return properties;
    }

    public static class GoalProperty {
        private String name;
        private String value;

        @XmlAttribute
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlAttribute
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}

package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * Set a JavaBean property inside a {@link org.lskk.lumen.reasoner.goal.Goal} object.
     */
    public static class GoalProperty {
        private String name;
        private String valueExpression;

        /**
         * JavaBean property name.
         * @return
         */
        @XmlAttribute
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        /**
         * JavaBean property MVEL expression, which will be evaluated using
         * {@link org.mvel2.templates.TemplateRuntime#eval(String, Map)}.
         *
         * <p>Available variables:</p>
         * <ol>
         *     <li>{@code groups}: Captured groups from AIML pattern</li>
         * </ol>
         * @return
         */
        @XmlAttribute(name = "value")
        public String getValueExpression() {
            return valueExpression;
        }

        public void setValueExpression(String valueExpression) {
            this.valueExpression = valueExpression;
        }
    }
}

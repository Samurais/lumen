package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by ceefour on 10/28/15.
 */
@XmlRootElement(name = "get")
public class Get implements Serializable {
    @XmlAttribute
    private String name;

    @Override
    public String toString() {
        return "Get{" +
                "name='" + name + '\'' +
                '}';
    }
}

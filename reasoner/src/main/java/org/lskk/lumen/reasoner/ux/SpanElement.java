package org.lskk.lumen.reasoner.ux;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by ceefour on 21/02/2016.
 */
@XmlRootElement(name = "span")
public class SpanElement implements Serializable {
    private String id;

    @XmlAttribute
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

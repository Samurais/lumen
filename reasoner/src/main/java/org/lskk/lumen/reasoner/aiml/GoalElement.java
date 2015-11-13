package org.lskk.lumen.reasoner.aiml;

import javax.xml.bind.annotation.XmlAttribute;
import java.io.Serializable;

/**
 * Created by ceefour on 07/11/2015.
 */
public class GoalElement implements Serializable {
    private String kind;

    @XmlAttribute
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}

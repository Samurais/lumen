package org.lskk.lumen.reasoner.ux;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 21/02/2016.
 */
@XmlRootElement(name = "interaction")
public class InteractionElement {

    @XmlElement(name="fragment")
    private List<FragmentElement> fragments = new ArrayList<>();

    public List<FragmentElement> getFragments() {
        return fragments;
    }
}

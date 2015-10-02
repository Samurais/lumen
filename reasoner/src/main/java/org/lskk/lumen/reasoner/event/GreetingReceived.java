package org.lskk.lumen.reasoner.event;

import java.io.Serializable;

/**
 * Created by ceefour on 10/2/15.
 */
public class GreetingReceived implements Serializable {
    private String fromName;

    public GreetingReceived(String fromName) {
        this.fromName = fromName;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
}

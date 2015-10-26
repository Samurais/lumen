package org.lskk.lumen.reasoner.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 26/10/2015.
 */
public class AgentResponse implements Serializable {

    private List<Object> stimulus = new ArrayList<>();
    private Object response;

    public AgentResponse(Object stimulus, Object response) {
        this.stimulus.add(stimulus);
        this.response = response;
    }

    public AgentResponse(List<Object> stimulus, Object response) {
        this.stimulus.addAll(stimulus);
        this.response = response;
    }

    public List<Object> getStimulus() {
        return stimulus;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "AgentResponse{" +
                "stimulus=" + stimulus +
                ", response=" + response +
                '}';
    }
}

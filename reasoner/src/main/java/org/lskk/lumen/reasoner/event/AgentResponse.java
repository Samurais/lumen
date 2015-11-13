package org.lskk.lumen.reasoner.event;

import org.lskk.lumen.core.CommunicateAction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 26/10/2015.
 */
public class AgentResponse implements Serializable {

    private List<Object> stimulus = new ArrayList<>();
    private CommunicateAction communicateAction;
    private List<Serializable> insertables = new ArrayList<>();

    public AgentResponse(Object stimulus, CommunicateAction communicateAction) {
        this.stimulus.add(stimulus);
        this.communicateAction = communicateAction;
    }

    public AgentResponse(List<Object> stimulus, CommunicateAction communicateAction) {
        this.stimulus.addAll(stimulus);
        this.communicateAction = communicateAction;
    }

    public List<Object> getStimulus() {
        return stimulus;
    }

    public Object getCommunicateAction() {
        return communicateAction;
    }

    public void setCommunicateAction(CommunicateAction communicateAction) {
        this.communicateAction = communicateAction;
    }

    /**
     * Events to be inserted.
     * @return
     */
    public List<Serializable> getInsertables() {
        return insertables;
    }

    @Override
    public String toString() {
        return "AgentResponse{" +
                "stimulus=" + stimulus +
                ", response=" + communicateAction +
                '}';
    }
}

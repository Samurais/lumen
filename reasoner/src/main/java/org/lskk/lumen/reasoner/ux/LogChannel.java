package org.lskk.lumen.reasoner.ux;

import org.lskk.lumen.core.CommunicateAction;
import org.springframework.stereotype.Service;

/**
 * Simply logs using SLF4J.
 * Created by ceefour on 14/11/2015.
 */
@Service
public class LogChannel extends Channel {

    @Override
    public void express(String avatarId, CommunicateAction communicateAction, Object params) {
        log.info("Expressing: {}", communicateAction);
    }

}

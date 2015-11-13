package org.lskk.lumen.reasoner.ux;

import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.expression.Proposition;

/**
 * Provides a way to interact with person, either physically or via social media.
 * Created by ceefour on 14/11/2015.
 */
public interface Channel {
    void express(CommunicateAction communicateAction);
    void express(Proposition proposition);
}

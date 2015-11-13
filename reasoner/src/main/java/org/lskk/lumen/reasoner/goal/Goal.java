package org.lskk.lumen.reasoner.goal;

import org.lskk.lumen.reasoner.ux.Channel;

import java.io.Serializable;

/**
 * Created by ceefour on 07/11/2015.
 */
public class Goal implements Serializable {
    private Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}

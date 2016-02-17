package org.lskk.lumen.reasoner.intent;

import org.lskk.lumen.reasoner.ux.Channel;

import java.io.Serializable;

/**
 * Created by ceefour on 07/11/2015.
 */
public class Intent implements Serializable {
    private Channel channel;
    private String avatarId;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }
}

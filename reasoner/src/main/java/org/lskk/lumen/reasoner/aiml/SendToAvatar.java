package org.lskk.lumen.reasoner.aiml;

import org.joda.time.Period;
import org.lskk.lumen.core.AvatarChannel;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Sends a JSON-LD {@link org.lskk.lumen.core.LumenThing} to an {@link org.lskk.lumen.core.AvatarChannel}.
 * Created by ceefour on 30/11/2015.
 */
public class SendToAvatar implements Serializable {

    private AvatarChannel channel;
    private String content;
    private Period delay;

    @XmlAttribute
    public AvatarChannel getChannel() {
        return channel;
    }

    public void setChannel(AvatarChannel channel) {
        this.channel = channel;
    }

    @XmlValue
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @XmlTransient
    public Period getDelay() {
        return delay;
    }

    public void setDelay(Period delay) {
        this.delay = delay;
    }

    @XmlAttribute(name = "delay")
    public String getDelayString() {
        return Objects.toString(delay, null);
    }

    public void setDelayString(String delay) {
        this.delay = Period.parse(delay);
    }

    public long getDelayMillis() {
        return Optional.ofNullable(delay).orElse(Period.ZERO).getMillis();
    }

    @Override
    public String toString() {
        return "SendToAvatar{" +
                "channel=" + channel +
                ", content='" + content + '\'' +
                '}';
    }
}

package org.lskk.lumen.reasoner.event;

import org.lskk.lumen.reasoner.aiml.SendToAvatar;

import java.io.Serializable;

/**
 * Created by ceefour on 30/11/2015.
 */
public class SemanticMessage implements Serializable {
    private String topic;
    private String content;
    private SendToAvatar sendToAvatar;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public SendToAvatar getSendToAvatar() {
        return sendToAvatar;
    }

    public void setSendToAvatar(SendToAvatar sendToAvatar) {
        this.sendToAvatar = sendToAvatar;
    }

    @Override
    public String toString() {
        return "SemanticMessage{" +
                "topic='" + topic + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}

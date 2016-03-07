package org.lskk.lumen.reasoner.skill;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.lskk.lumen.reasoner.activity.Activity;

import java.io.Serializable;
import java.net.URI;

/**
 * Created by ceefour on 03/03/2016.
 */
public class ActivityRef implements Serializable {
    private String href;
    private Boolean intentCapturing;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getId() {
        return URI.create(href).getSchemeSpecificPart();
    }

    public String getScheme() {
        return URI.create(href).getScheme();
    }

    /**
     * An intent-capturing {@link Activity} can match
     * {@link org.lskk.lumen.reasoner.activity.UtterancePattern.Scope#GLOBAL} utterances
     * even when the {@link Activity} is not yet asserted
     * in the {@link org.lskk.lumen.reasoner.activity.InteractionSession}.
     * @return
     */
    public Boolean getIntentCapturing() {
        return intentCapturing;
    }

    public void setIntentCapturing(Boolean intentCapturing) {
        this.intentCapturing = intentCapturing;
    }
}

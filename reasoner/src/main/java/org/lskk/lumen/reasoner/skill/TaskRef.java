package org.lskk.lumen.reasoner.skill;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.net.URI;

/**
 * Created by ceefour on 03/03/2016.
 */
public class TaskRef implements Serializable {
    private String href;
    private Boolean intentCapturing;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @JsonIgnore
    public String getId() {
        return URI.create(href).getSchemeSpecificPart();
    }

    @JsonIgnore
    public String getScheme() {
        return URI.create(href).getScheme();
    }

    /**
     * An intent-capturing {@link org.lskk.lumen.reasoner.interaction.InteractionTask} can match
     * {@link org.lskk.lumen.reasoner.interaction.UtterancePattern.Scope#GLOBAL} utterances
     * even when the {@link org.lskk.lumen.reasoner.interaction.InteractionTask} is not yet asserted
     * in the {@link org.lskk.lumen.reasoner.interaction.InteractionSession}.
     * @return
     */
    public Boolean getIntentCapturing() {
        return intentCapturing;
    }

    public void setIntentCapturing(Boolean intentCapturing) {
        this.intentCapturing = intentCapturing;
    }
}

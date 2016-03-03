package org.lskk.lumen.reasoner.skill;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * Flow-based programming-style connection between {@link Activity}s.
 * Created by ceefour on 04/03/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection implements Serializable {
    private String source;
    private String sink;

    /**
     * Source out slot, using [activity].[slot] syntax, e.g. {@code promptMeasurementToUnit.measure}.
     * @return
     */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Sink in slot, using [activity].[slot] syntax, e.g. {@code affirmConvertUnit.measure}.
     * @return
     */
    public String getSink() {
        return sink;
    }

    public void setSink(String sink) {
        this.sink = sink;
    }

    @JsonIgnore
    public String getSourceActivity() {
        return StringUtils.substringBefore(source, ".");
    }

    @JsonIgnore
    public String getSourceSlot() {
        return StringUtils.substringAfter(source, ".");
    }

    @JsonIgnore
    public String getSinkActivity() {
        return StringUtils.substringBefore(sink, ".");
    }

    @JsonIgnore
    public String getSinkSlot() {
        return StringUtils.substringAfter(sink, ".");
    }
}

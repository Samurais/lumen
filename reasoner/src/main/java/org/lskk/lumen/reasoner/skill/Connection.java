package org.lskk.lumen.reasoner.skill;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;
import org.lskk.lumen.reasoner.activity.Activity;

import java.io.Serializable;

/**
 * Flow-based programming-style connection between {@link Activity}s.
 * Created by ceefour on 04/03/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection implements Serializable {
    private String source;
    private String sink;

    public Connection(@JsonProperty("source") String source, @JsonProperty("sink") String sink) {
        this.source = source;
        this.sink = sink;
    }

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

    public String getSourceActivity() {
        return StringUtils.substringBefore(source, ".");
    }

    public String getSourceSlot() {
        return StringUtils.substringAfter(source, ".");
    }

    public String getSinkActivity() {
        return StringUtils.substringBefore(sink, ".");
    }

    public String getSinkSlot() {
        return StringUtils.substringAfter(sink, ".");
    }

    public String getActivityPair() {
        return getSourceActivity() + "," + getSinkActivity();
    }

}

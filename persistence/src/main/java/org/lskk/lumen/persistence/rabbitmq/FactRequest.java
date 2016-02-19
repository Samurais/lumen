package org.lskk.lumen.persistence.rabbitmq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;
import org.lskk.lumen.persistence.web.FactServiceOperation;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

/**
 * Created by ceefour on 19/02/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FactRequest implements Serializable {
    private FactServiceOperation operation;

    private String upLabel;
    private Locale inLanguage;
    private Map<String, Float> contexts;
    private String nodeName;
    private Boolean isPrefLabel;
    private String property;
    private String objectNodeName;
    private float[] truthValue;
    private DateTime assertionTime;
    private String asserterNodeName;

    public FactServiceOperation getOperation() {
        return operation;
    }

    public void setOperation(FactServiceOperation operation) {
        this.operation = operation;
    }

    public String getUpLabel() {
        return upLabel;
    }

    public void setUpLabel(String upLabel) {
        this.upLabel = upLabel;
    }

    public Locale getInLanguage() {
        return inLanguage;
    }

    public void setInLanguage(Locale inLanguage) {
        this.inLanguage = inLanguage;
    }

    public Map<String, Float> getContexts() {
        return contexts;
    }

    public void setContexts(Map<String, Float> contexts) {
        this.contexts = contexts;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Boolean getPrefLabel() {
        return isPrefLabel;
    }

    public void setPrefLabel(Boolean prefLabel) {
        isPrefLabel = prefLabel;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getObjectNodeName() {
        return objectNodeName;
    }

    public void setObjectNodeName(String objectNodeName) {
        this.objectNodeName = objectNodeName;
    }

    public float[] getTruthValue() {
        return truthValue;
    }

    public void setTruthValue(float[] truthValue) {
        this.truthValue = truthValue;
    }

    public DateTime getAssertionTime() {
        return assertionTime;
    }

    public void setAssertionTime(DateTime assertionTime) {
        this.assertionTime = assertionTime;
    }

    public String getAsserterNodeName() {
        return asserterNodeName;
    }

    public void setAsserterNodeName(String asserterNodeName) {
        this.asserterNodeName = asserterNodeName;
    }
}

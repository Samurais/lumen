package org.lskk.lumen.persistence.service;

import org.joda.time.DateTime;
import org.lskk.lumen.persistence.neo4j.Thing;

import java.io.Serializable;
import java.util.Locale;

/**
 * Created by ceefour on 21/02/2016.
 */
public class Fact implements Serializable {
    private Thing subject;
    private String property;
    private String objectAsText;
    private DateTime objectAsDateTime;
    private Locale objectLanguage;

    public Thing getSubject() {
        return subject;
    }

    public void setSubject(Thing subject) {
        this.subject = subject;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getObjectAsText() {
        return objectAsText;
    }

    public void setObjectAsText(String objectAsText) {
        this.objectAsText = objectAsText;
    }

    public DateTime getObjectAsDateTime() {
        return objectAsDateTime;
    }

    public void setObjectAsDateTime(DateTime objectAsDateTime) {
        this.objectAsDateTime = objectAsDateTime;
    }

    public Locale getObjectLanguage() {
        return objectLanguage;
    }

    public void setObjectLanguage(Locale objectLanguage) {
        this.objectLanguage = objectLanguage;
    }

    @Override
    public String toString() {
        return "Fact{" +
                "subject=" + subject +
                ", property='" + property + '\'' +
                ", objectAsText='" + objectAsText + '\'' +
                ", objectAsDateTime=" + objectAsDateTime +
                ", objectLanguage=" + objectLanguage +
                '}';
    }
}

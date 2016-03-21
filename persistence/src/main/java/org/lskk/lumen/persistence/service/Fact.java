package org.lskk.lumen.persistence.service;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.lskk.lumen.persistence.neo4j.Thing;

import java.io.Serializable;
import java.util.Locale;

/**
 * Created by ceefour on 21/02/2016.
 */
public class Fact implements Serializable {

    private Thing subject;
    private String property;
    private FactKind kind;
    private String objectAsString;
    private Locale objectLanguage;
    private DateTime objectAsDateTime;
    private LocalDate objectAsLocalDate;

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

    public String getObjectAsString() {
        return objectAsString;
    }

    public void setObjectAsString(String objectAsString) {
        this.objectAsString = objectAsString;
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

    public FactKind getKind() {
        return kind;
    }

    public void setKind(FactKind kind) {
        this.kind = kind;
    }

    public LocalDate getObjectAsLocalDate() {
        return objectAsLocalDate;
    }

    public void setObjectAsLocalDate(LocalDate objectAsLocalDate) {
        this.objectAsLocalDate = objectAsLocalDate;
    }

    @Override
    public String toString() {
        return "Fact{" +
                "subject=" + subject +
                ", property='" + property + '\'' +
                ", kind=" + kind +
                ", objectAsString='" + objectAsString + '\'' +
                ", objectLanguage=" + objectLanguage +
                ", objectAsDateTime=" + objectAsDateTime +
                ", objectAsLocalDate=" + objectAsLocalDate +
                '}';
    }
}

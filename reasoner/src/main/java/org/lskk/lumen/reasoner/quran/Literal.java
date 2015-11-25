package org.lskk.lumen.reasoner.quran;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Created by aina on 25/11/2015.
 */
@Entity
@Table(schema="sanad")
public class Literal implements Serializable {
    @Id
    private String id;
    private String description;
    private String descriptionHtml;
    @Column(columnDefinition = "text")
    private String adoc;
    @Column(columnDefinition = "text")
    private String html;
    private String inLanguage;
    @Column(columnDefinition = "text")
    private String normalized;
    private String numeronym;
    private String translator;
    private String creativeWork_id;
    private String articleBodies_thing_id;
    private String name_thing_id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    public void setDescriptionHtml(String descriptionHtml) {
        this.descriptionHtml = descriptionHtml;
    }

    public String getAdoc() {
        return adoc;
    }

    public void setAdoc(String adoc) {
        this.adoc = adoc;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getInLanguage() {
        return inLanguage;
    }

    public void setInLanguage(String inLanguage) {
        this.inLanguage = inLanguage;
    }

    public String getNormalized() {
        return normalized;
    }

    public void setNormalized(String normalized) {
        this.normalized = normalized;
    }

    public String getNumeronym() {
        return numeronym;
    }

    public void setNumeronym(String numeronym) {
        this.numeronym = numeronym;
    }

    public String getTranslator() {
        return translator;
    }

    public void setTranslator(String translator) {
        this.translator = translator;
    }

    public String getCreativeWork_id() {
        return creativeWork_id;
    }

    public void setCreativeWork_id(String creativeWork_id) {
        this.creativeWork_id = creativeWork_id;
    }

    public String getArticleBodies_thing_id() {
        return articleBodies_thing_id;
    }

    public void setArticleBodies_thing_id(String articleBodies_thing_id) {
        this.articleBodies_thing_id = articleBodies_thing_id;
    }

    public String getName_thing_id() {
        return name_thing_id;
    }

    public void setName_thing_id(String name_thing_id) {
        this.name_thing_id = name_thing_id;
    }

    @Override
    public String toString() {
        return "Literal{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", descriptionHtml='" + descriptionHtml + '\'' +
                ", adoc='" + adoc + '\'' +
                ", html='" + html + '\'' +
                ", inLanguage='" + inLanguage + '\'' +
                ", normalized='" + normalized + '\'' +
                ", numeronym='" + numeronym + '\'' +
                ", translator='" + translator + '\'' +
                ", creativeWork_id='" + creativeWork_id + '\'' +
                ", articleBodies_thing_id='" + articleBodies_thing_id + '\'' +
                ", name_thing_id='" + name_thing_id + '\'' +
                '}';
    }
}

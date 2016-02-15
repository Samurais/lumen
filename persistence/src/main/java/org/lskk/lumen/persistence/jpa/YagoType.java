package org.lskk.lumen.persistence.jpa;

import org.lskk.lumen.persistence.neo4j.Thing;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 13/02/2016.
 */
@Entity
public class YagoType implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true, length = 4000)
    private String nn;
    @Column(length = 4000)
    private String prefLabel;
    @Column(length = 4000)
    private String isPreferredMeaningOf;
    // type entities don't use them
//    @Column
//    private String hasGivenName;
//    @Column
//    private String hasFamilyName;
//    @Column(length = 4000)
//    private String redirectedFrom;
    @Column(columnDefinition = "text")
    private String hasGloss;
    @ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "yagotype_superclasses",
            joinColumns = @JoinColumn(name = "yagotype_id"),
            inverseJoinColumns = @JoinColumn(name = "superclass_yagotype_id"))
    private List<YagoType> superClasses = new ArrayList<>();
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "superClasses")
    private List<YagoType> subClasses = new ArrayList<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "type")
    private List<YagoLabel> labels = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNn() {
        return nn;
    }

    public void setNn(String nn) {
        this.nn = nn;
    }

    public String getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }

    public String getIsPreferredMeaningOf() {
        return isPreferredMeaningOf;
    }

    public void setIsPreferredMeaningOf(String isPreferredMeaningOf) {
        this.isPreferredMeaningOf = isPreferredMeaningOf;
    }

    public List<YagoType> getSuperClasses() {
        return superClasses;
    }

    public List<YagoType> getSubClasses() {
        return subClasses;
    }

    public List<YagoLabel> getLabels() {
        return labels;
    }

    //    public String getHasGivenName() {
//        return hasGivenName;
//    }
//
//    public void setHasGivenName(String hasGivenName) {
//        this.hasGivenName = hasGivenName;
//    }
//
//    public String getHasFamilyName() {
//        return hasFamilyName;
//    }
//
//    public void setHasFamilyName(String hasFamilyName) {
//        this.hasFamilyName = hasFamilyName;
//    }
//
//    public String getRedirectedFrom() {
//        return redirectedFrom;
//    }
//
//    public void setRedirectedFrom(String redirectedFrom) {
//        this.redirectedFrom = redirectedFrom;
//    }

    public String getHasGloss() {
        return hasGloss;
    }

    public void setHasGloss(String hasGloss) {
        this.hasGloss = hasGloss;
    }

    @Override
    public String toString() {
        return nn;
    }

    public Thing toThingFull() {
        final Thing thing = toThingCompact();
        thing.getSuperClasses().addAll(getSuperClasses().stream().map(YagoType::toThingCompact).collect(Collectors.toList()));
        return thing;
    }

    public Thing toThingCompact() {
        final Thing thing = new Thing();
        thing.setNn(getNn());
        thing.setPrefLabel(getPrefLabel());
        thing.setPrefLabelLang(Locale.US.toLanguageTag());
        thing.setIsPreferredMeaningOf(getIsPreferredMeaningOf());
        return thing;
    }
}

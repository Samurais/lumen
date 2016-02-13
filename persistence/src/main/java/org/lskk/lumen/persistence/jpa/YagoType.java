package org.lskk.lumen.persistence.jpa;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
}

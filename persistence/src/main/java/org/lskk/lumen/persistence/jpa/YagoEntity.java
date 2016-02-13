package org.lskk.lumen.persistence.jpa;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ceefour on 13/02/2016.
 * @todo Rename to YagoType, because not all entities are here
 */
@Entity
public class YagoEntity implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true, length = 4000)
    private String nn;
    @Column(length = 4000)
    private String prefLabel;
    @Column(length = 4000)
    private String isPreferredMeaningOf;
    @Column
    private String hasGivenName;
    @Column
    private String hasFamilyName;
    @Column(length = 4000)
    private String redirectedFrom;
    @Column(columnDefinition = "text")
    private String hasGloss;
    @ManyToMany(fetch = FetchType.LAZY) @JoinTable(name = "yagoentity_superclasses",
        inverseJoinColumns = @JoinColumn(name = "superclass_yagoentity_id"))
    private Set<YagoEntity> superClasses = new HashSet<>();

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

    public Set<YagoEntity> getSuperClasses() {
        return superClasses;
    }

    public String getHasGivenName() {
        return hasGivenName;
    }

    public void setHasGivenName(String hasGivenName) {
        this.hasGivenName = hasGivenName;
    }

    public String getHasFamilyName() {
        return hasFamilyName;
    }

    public void setHasFamilyName(String hasFamilyName) {
        this.hasFamilyName = hasFamilyName;
    }

    public String getRedirectedFrom() {
        return redirectedFrom;
    }

    public void setRedirectedFrom(String redirectedFrom) {
        this.redirectedFrom = redirectedFrom;
    }

    public String getHasGloss() {
        return hasGloss;
    }

    public void setHasGloss(String hasGloss) {
        this.hasGloss = hasGloss;
    }
}

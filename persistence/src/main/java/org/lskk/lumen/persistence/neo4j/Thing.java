package org.lskk.lumen.persistence.neo4j;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Indexes:
 * CREATE INDEX ON :schema_Thing(_partition);
 * CREATE INDEX ON :schema_Thing(nn);
 * CREATE INDEX ON :schema_Thing(prefLabel);
 *
 * We actually need a compound unique constraint on _partition+nn, but since it's not available then just use
 * normal indexes.
 *
 * Created by ceefour on 14/02/2016.
 */
@NodeEntity(label = "schema_Thing")
@Ensure("CREATE INDEX ON :schema_Thing(_partition)")
@Ensure("CREATE INDEX ON :schema_Thing(nn)")
@Ensure("CREATE INDEX ON :schema_Thing(prefLabel)")
public class Thing implements Serializable {

    @GraphId
    private Long gid;
    @Property(name = "nn")
    private String nn;
    private String prefLabel;
    private String prefLabelLang;
    private String isPreferredMeaningOf;
    @Property(name = "_partition")
    private PartitionKey partition;
    @Relationship(type = "rdf_type")
    private Set<Thing> types = new HashSet<>();
    @Relationship(type = "rdfs_subClassOf")
    private Set<Thing> superClasses = new HashSet<>();
    @Relationship(type = "rdfs_subClassOf", direction = "INCOMING")
    private Set<Thing> subClasses = new HashSet<>();

    public Long getGid() {
        return gid;
    }

    public void setGid(Long gid) {
        this.gid = gid;
    }

    /**
     * For YAGO things, the partition is
     * "lumen_yago".
     * For user-modifiable things, the partition is
     * "lumen_var".
     * Why not with [tenantEnv]? To make it easy to import/export.
     * @return
     */
    public PartitionKey getPartition() {
        return partition;
    }

    public void setPartition(PartitionKey partition) {
        this.partition = partition;
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

    /**
     * Language of the {@link #getPrefLabel()} (if set).
     * {@code null} means unknown.
     * The default is {@link java.util.Locale#US} and things should have this set.
     * @return
     */
    public String getPrefLabelLang() {
        return prefLabelLang;
    }

    public void setPrefLabelLang(String prefLabelLang) {
        this.prefLabelLang = prefLabelLang;
    }

    public String getIsPreferredMeaningOf() {
        return isPreferredMeaningOf;
    }

    public void setIsPreferredMeaningOf(String isPreferredMeaningOf) {
        this.isPreferredMeaningOf = isPreferredMeaningOf;
    }

    public Set<Thing> getTypes() {
        return types;
    }

    public Set<Thing> getSuperClasses() {
        return superClasses;
    }

    public Set<Thing> getSubClasses() {
        return subClasses;
    }

    @Override
    public String toString() {
        return "Thing{" +
                "gid=" + gid +
                ", nn='" + nn + '\'' +
                ", prefLabel='" + prefLabel + '\'' +
                ", prefLabelLang='" + prefLabelLang + '\'' +
                ", isPreferredMeaningOf='" + isPreferredMeaningOf + '\'' +
                ", partition=" + partition +
                ", types=" + types +
                '}';
    }
}

package org.lskk.lumen.persistence.neo4j;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

import java.io.Serializable;

/**
 * Created by ceefour on 22/02/2016.
 */
@NodeEntity(label = "lumen_Label")
@Ensure("CREATE INDEX ON :lumen_Label(_partition)")
@Ensure("CREATE INDEX ON :lumen_Label(l)")
@Ensure("CREATE INDEX ON :lumen_Label(v)")
@Ensure("CREATE INDEX ON :lumen_Label(m)")
public class ThingLabel implements Serializable {
    @GraphId
    private Long gid;
    @Property(name = "l")
    private String inLanguage;
    @Property(name = "v")
    private String value;
    @Property(name = "m")
    private String metaphone;
    @Property(name = "_partition")
    private PartitionKey partition;

    public Long getGid() {
        return gid;
    }

    public void setGid(Long gid) {
        this.gid = gid;
    }

    public String getInLanguage() {
        return inLanguage;
    }

    public void setInLanguage(String inLanguage) {
        this.inLanguage = inLanguage;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public PartitionKey getPartition() {
        return partition;
    }

    public void setPartition(PartitionKey partition) {
        this.partition = partition;
    }

    /**
     * Metaphone with a maximum of 15 characters.
     *
     * @return
     */
    public String getMetaphone() {
        return metaphone;
    }

    public void setMetaphone(String metaphone) {
        this.metaphone = metaphone;
    }
}

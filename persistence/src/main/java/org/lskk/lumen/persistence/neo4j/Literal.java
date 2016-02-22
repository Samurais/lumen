package org.lskk.lumen.persistence.neo4j;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

import java.io.Serializable;

/**
 * Created by ceefour on 22/02/2016.
 */
@NodeEntity(label = "rdfs_Literal")
@Ensure("CREATE INDEX ON :rdfs_Literal(_partition)")
public class Literal implements Serializable {

    @GraphId
    private Long gid;
    @Property(name = "t")
    private String type;
    @Relationship(type = "rdf_subject")
    private Thing subject;
    @Relationship(type = "rdf_predicate")
    private SemanticProperty predicate;
    @Property(name = "_partition")
    private PartitionKey partition;
    @Property(name = "v")
    private Object value;

    public Long getGid() {
        return gid;
    }

    public void setGid(Long gid) {
        this.gid = gid;
    }

    public Thing getSubject() {
        return subject;
    }

    public void setSubject(Thing subject) {
        this.subject = subject;
    }

    public SemanticProperty getPredicate() {
        return predicate;
    }

    public void setPredicate(SemanticProperty predicate) {
        this.predicate = predicate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PartitionKey getPartition() {
        return partition;
    }

    public void setPartition(PartitionKey partition) {
        this.partition = partition;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}

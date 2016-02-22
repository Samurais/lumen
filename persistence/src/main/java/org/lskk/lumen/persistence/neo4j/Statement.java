package org.lskk.lumen.persistence.neo4j;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * For literal objects, use {@link Literal} instead.
 * Created by ceefour on 22/02/2016.
 */
@NodeEntity(label = "rdf_Statement")
@Ensure("CREATE INDEX ON :rdf_Statement(_partition)")
public class Statement implements Serializable {

    @GraphId
    private Long gid;
    @Property(name = "_partition")
    private PartitionKey partition;
    @Relationship(type = "rdf_subject")
    private Thing subject;
    @Relationship(type = "rdf_predicate")
    private SemanticProperty predicate;
    @Relationship(type = "rdf_object", direction = "INCOMING")
    private Thing object;

    public Long getGid() {
        return gid;
    }

    public void setGid(Long gid) {
        this.gid = gid;
    }

    public PartitionKey getPartition() {
        return partition;
    }

    public void setPartition(PartitionKey partition) {
        this.partition = partition;
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

    public Thing getObject() {
        return object;
    }

    public void setObject(Thing object) {
        this.object = object;
    }
}

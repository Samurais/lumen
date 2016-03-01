package org.lskk.lumen.persistence.neo4j;

import com.google.common.base.Preconditions;
import org.apache.jena.vocabulary.RDF;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents {@link RDF#Property}.
 * Created by ceefour on 22/02/2016.
 */
@NodeEntity(label = "rdf_Property")
@Ensure("CREATE INDEX ON :rdf_Property(_partition)")
@Ensure("CREATE INDEX ON :rdf_Property(nn)")
@Ensure("CREATE INDEX ON :rdf_Property(label)")
public class SemanticProperty implements Serializable {

    @GraphId
    private Long gid;
    @Property(name = "_partition")
    private PartitionKey partition;
    @Property(name = "nn")
    private String nn;
    @Property(name = "label")
    private String name;
    @Relationship(type = "rdfs:domain")
    private Set<Thing> domains = new HashSet<>();
    @Relationship(type = "rdfs:range")
    private Set<Thing> ranges = new HashSet<>();

    public static SemanticProperty forYago(String nn) {
        Preconditions.checkArgument(nn.startsWith("yago:"), "%s is not a \"yago:\" property", nn);
        final SemanticProperty semanticProperty = new SemanticProperty();
        semanticProperty.setPartition(PartitionKey.lumen_yago);
        semanticProperty.setNn(nn);
        return semanticProperty;
    }

    public static SemanticProperty forLumen(String nn) {
        Preconditions.checkArgument(nn.startsWith("lumen:"), "%s is not a \"lumen:\" property", nn);
        final SemanticProperty semanticProperty = new SemanticProperty();
        semanticProperty.setPartition(PartitionKey.lumen_platform);
        semanticProperty.setNn(nn);
        return semanticProperty;
    }

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

    public String getNn() {
        return nn;
    }

    public void setNn(String nn) {
        this.nn = nn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Must point to {@link org.apache.jena.vocabulary.RDFS#Class} things.
     *
     * @return
     */
    public Set<Thing> getDomains() {
        return domains;
    }

    /**
     * Must point to {@link org.apache.jena.vocabulary.RDFS#Class} things.
     *
     * @return
     */
    public Set<Thing> getRanges() {
        return ranges;
    }

}

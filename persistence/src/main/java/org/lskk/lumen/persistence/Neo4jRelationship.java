package org.lskk.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.neo4j.graphdb.Relationship;

import java.util.Map;

/**
 * JSON-friendly Neo4j {@link Relationship}.
 * Created on 2/4/15.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = Neo4jRelationship.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "Neo4jRelationship", value = Neo4jRelationship.class))
public class Neo4jRelationship {
    public Neo4jRelationship(Long relId, String relType, Map<String, Object> properties) {
        this.relId = relId;
        this.relTypeName = relType;
        this.properties = ImmutableMap.copyOf(properties);
    }

    public Neo4jRelationship(final Relationship rel) {
        this(rel.getId(), rel.getType().name(),
                Maps.asMap(ImmutableSet.copyOf(rel.getPropertyKeys()), k -> rel.getProperty(k)));
    }

    public Long getRelId() {
        return relId;
    }

    public void setRelId(Long relId) {
        this.relId = relId;
    }

    public String getRelTypeName() {
        return relTypeName;
    }

    public void setRelTypeName(String relTypeName) {
        this.relTypeName = relTypeName;
    }

    public ImmutableMap<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(ImmutableMap<String, Object> properties) {
        this.properties = properties;
    }

    @JsonProperty("id")
    private Long relId;
    private String relTypeName;
    private ImmutableMap<String, Object> properties;
}

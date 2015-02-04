package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.neo4j.graphdb.Relationship

/**
 * JSON-friendly Neo4j {@link org.neo4j.graphdb.Relationship}.
 * Created on 2/4/15.
 */
@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type", defaultImpl=Neo4jRelationship.class)
@JsonSubTypes(@JsonSubTypes.Type(name="Neo4jRelationship", value=Neo4jRelationship.class))
class Neo4jRelationship {
    @JsonProperty('id')
    Long relationshipId
    ImmutableMap<String, Object> properties

    Neo4jRelationship(Long relationshipId, Map<String, Object> properties) {
        this.relationshipId = relationshipId
        this.properties = ImmutableMap.copyOf(properties)
    }

    Neo4jRelationship(Relationship rel) {
        this.relationshipId = rel.id
        this.properties = ImmutableMap.copyOf(rel.properties)
    }

}

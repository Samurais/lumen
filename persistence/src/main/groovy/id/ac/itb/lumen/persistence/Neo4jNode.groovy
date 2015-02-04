package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship

/**
 * JSON-friendly Neo4j Node.
 * Created on 2/4/15.
 */
@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type", defaultImpl=Neo4jNode.class)
@JsonSubTypes(@JsonSubTypes.Type(name="Neo4jNode", value=Neo4jNode.class))
class Neo4jNode {
    @JsonProperty('id')
    Long nodeId
    Map<String, Object> properties

    Neo4jNode(Long nodeId, Map<String, Object> properties) {
        this.nodeId = nodeId
        this.properties = properties
    }

    Neo4jNode(Node node) {
        this.nodeId = node.id
        this.properties = ImmutableMap.copyOf(node.properties)
    }

}

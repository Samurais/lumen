package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
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
    Set<String> labels = []
    Map<String, Object> properties = [:]

    Neo4jNode(Long nodeId, Set<String> labels, Map<String, Object> properties) {
        this.nodeId = nodeId
        this.labels = ImmutableSet.copyOf(labels)
        this.properties = properties
        // Avoid "No serializer found for class org.neo4j.graphdb.DynamicLabel"
//                ImmutableMap.copyOf(properties
//                .findAll { k, v -> !['relationships', 'graphDatabase', 'relationshipTypes'].contains(k) }
//                .collectEntries { k, v ->
//            if ('labels'.equals(k)) {
//                [k, v.collect { it as String }]
//            } else {
//                [k, v]
//            }
//        } as Map<String, Object>)
    }

    Neo4jNode(Node node) {
        this(node.id, node.labels.collect { it as String }.toSet(),
                node.getPropertyKeys().collectEntries {
            [it, node.getProperty(it)]
        })
    }

}

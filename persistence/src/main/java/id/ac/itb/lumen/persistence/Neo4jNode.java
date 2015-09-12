package id.ac.itb.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableSet;
import groovy.lang.Closure;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.neo4j.graphdb.Node;

import java.util.*;

/**
 * JSON-friendly Neo4j Node.
 * Created on 2/4/15.
 */
@CompileStatic
@JsonInclude(JsonInclude.com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = Neo4jNode.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "Neo4jNode", value = Neo4jNode.class))
public class Neo4jNode {
    public Neo4jNode(Long nodeId, Set<String> labels, Map<String, Object> properties) {
        this.nodeId = nodeId;
        this.labels = ImmutableSet.copyOf(labels);
        this.properties = properties;
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

    public Neo4jNode(final Node node) {
        this(node.getId(), DefaultGroovyMethods.toSet(DefaultGroovyMethods.collect(node.getLabels(), new Closure<String>(this, this) {
            public String doCall(Object it) {
                return (String) it;
            }

            public String doCall() {
                return doCall(null);
            }

        })), DefaultGroovyMethods.collectEntries(node.getPropertyKeys(), new Closure<ArrayList<Object>>(this, this) {
            public ArrayList<Object> doCall(String it) {
                return new ArrayList<Object>(Arrays.asList(it, node.getProperty(it)));
            }

            public ArrayList<Object> doCall() {
                return doCall(null);
            }

        }));
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public void setLabels(Set<String> labels) {
        this.labels = labels;
    }

    public LinkedHashMap<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @JsonProperty("id")
    private Long nodeId;
    private Set<String> labels = new ArrayList();
    private Map<String, Object> properties = new LinkedHashMap<String, Object>();
}

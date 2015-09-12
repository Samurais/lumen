package id.ac.itb.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableMap;
import groovy.lang.Closure;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * JSON-friendly Neo4j {@link Relationship}.
 * Created on 2/4/15.
 */
@CompileStatic
@JsonInclude(JsonInclude.com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = Neo4jRelationship.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "Neo4jRelationship", value = Neo4jRelationship.class))
public class Neo4jRelationship {
    public Neo4jRelationship(Long relId, String relType, Map<String, Object> properties) {
        this.relId = relId;
        this.relTypeName = relType;
        this.properties = ImmutableMap.copyOf(properties);
    }

    public Neo4jRelationship(final Relationship rel) {
        this(rel.getId(), rel.getType().name(), DefaultGroovyMethods.collectEntries(rel.getPropertyKeys(), new Closure<ArrayList<Object>>(this, this) {
            public ArrayList<Object> doCall(String it) {
                return new ArrayList<Object>(Arrays.asList(it, rel.getProperty(it)));
            }

            public ArrayList<Object> doCall() {
                return doCall(null);
            }

        }));
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

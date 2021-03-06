package org.lskk.lumen.persistence;

import org.lskk.lumen.persistence.neo4j.Thing;
import org.neo4j.ogm.annotation.GraphId;

import java.util.Set;

/**
 * Created by Budhi on 21/01/2015.
 * @deprecated Use {@link Thing}
 */
//@NodeEntity
@Deprecated
public class Entity {
    @Override
    public String toString() {
        return "Person{" + "nodeId=" + nodeId + ", uri='" + uri + "\'" + ", label='" + label + "\'" + "}";
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getqName() {
        return qName;
    }

    public void setqName(String qName) {
        this.qName = qName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }

    public Set<String> getNodeLabels() {
        return nodeLabels;
    }

    public void setNodeLabels(Set<String> nodeLabels) {
        this.nodeLabels = nodeLabels;
    }

    @GraphId
    private Long nodeId;
    private String uri;
    /**
     * Make it more convenient to visualize, and only for visualization purpose (that's why it's not indexed).
     */
    private String qName;
    /**
     * Make it more convenient to visualize, and only for visualization purpose (that's why it's not indexed).
     */
    private String label;
    /**
     * Make it more convenient to visualize and ad-hoc output, and only for visualization purpose (that's why it's not indexed).
     */
    private String prefLabel;
    private Set<String> nodeLabels;
}

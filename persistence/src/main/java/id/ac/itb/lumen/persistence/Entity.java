package id.ac.itb.lumen.persistence;

import groovy.transform.CompileStatic;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.Labels;
import org.springframework.data.neo4j.annotation.NodeEntity;

import java.util.Set;

/**
 * Created by Budhi on 21/01/2015.
 */
@CompileStatic
@NodeEntity
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
    @Indexed(unique = true)
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
    @Labels
    private Set<String> nodeLabels;
}

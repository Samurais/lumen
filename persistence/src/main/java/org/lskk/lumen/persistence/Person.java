package org.lskk.lumen.persistence;

import groovy.transform.CompileStatic;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * Created by Budhi on 21/01/2015.
 */
@CompileStatic
@NodeEntity
@Deprecated
public class Person {
    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }

    @GraphId
    private Long nodeId;
    @Indexed(unique = true)
    private String href;
    private String prefLabel;
}

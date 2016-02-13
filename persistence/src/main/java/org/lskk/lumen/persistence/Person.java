package org.lskk.lumen.persistence;

import org.lskk.lumen.persistence.neo4j.Thing;
import org.neo4j.ogm.annotation.GraphId;

/**
 * Created by Budhi on 21/01/2015.
 * @deprecated Use {@link Thing}.
 */
//@NodeEntity
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
    private String href;
    private String prefLabel;
}

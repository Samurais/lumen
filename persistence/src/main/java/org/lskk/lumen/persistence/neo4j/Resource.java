package org.lskk.lumen.persistence.neo4j;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * Created by Budhi on 21/01/2015.
 */
@NodeEntity
public class Resource {
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

    public String getIsPreferredMeaningOf() {
        return isPreferredMeaningOf;
    }

    public void setIsPreferredMeaningOf(String isPreferredMeaningOf) {
        this.isPreferredMeaningOf = isPreferredMeaningOf;
    }

    @GraphId
    private Long nodeId;
    @Indexed(unique = true)
    private String href;
    @Indexed
    private String prefLabel;
    @Indexed
    private String isPreferredMeaningOf;
}

package id.ac.itb.lumen.persistence

import groovy.transform.CompileStatic
import org.springframework.data.neo4j.annotation.GraphId
import org.springframework.data.neo4j.annotation.Indexed
import org.springframework.data.neo4j.annotation.Labels
import org.springframework.data.neo4j.annotation.NodeEntity

/**
 * Created by Budhi on 21/01/2015.
 */
@CompileStatic
@NodeEntity
class Entity {
    @GraphId
    Long nodeId
    @Indexed(unique = true)
    String uri
    /**
     * Make it more convenient to visualize, and only for visualization purpose (that's why it's not indexed).
     */
    String qName
    /**
     * Make it more convenient to visualize, and only for visualization purpose (that's why it's not indexed).
     */
    String label
    @Labels
    Set<String> nodeLabels

    @Override
    public String toString() {
        return "Person{" +
                "nodeId=" + nodeId +
                ", uri='" + uri + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}

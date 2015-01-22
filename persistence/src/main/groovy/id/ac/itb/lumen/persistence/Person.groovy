package id.ac.itb.lumen.persistence

import groovy.transform.CompileStatic
import org.springframework.data.neo4j.annotation.GraphId
import org.springframework.data.neo4j.annotation.Indexed
import org.springframework.data.neo4j.annotation.NodeEntity

/**
 * Created by Budhi on 21/01/2015.
 */
@CompileStatic
@NodeEntity
class Person {
    @GraphId
    Long nodeId
    @Indexed(unique = true)
    String uri
    String label

    @Override
    public String toString() {
        return "Person{" +
                "nodeId=" + nodeId +
                ", uri='" + uri + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}

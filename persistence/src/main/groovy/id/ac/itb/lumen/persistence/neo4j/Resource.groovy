package id.ac.itb.lumen.persistence.neo4j

import groovy.transform.CompileStatic
import org.springframework.data.neo4j.annotation.GraphId
import org.springframework.data.neo4j.annotation.Indexed
import org.springframework.data.neo4j.annotation.NodeEntity

/**
 * Created by Budhi on 21/01/2015.
 */
@CompileStatic
@NodeEntity
class Resource {
    @GraphId
    Long nodeId
    @Indexed(unique = true)
    String href
    @Indexed
    String prefLabel
    @Indexed
    String isPreferredMeaningOf
}

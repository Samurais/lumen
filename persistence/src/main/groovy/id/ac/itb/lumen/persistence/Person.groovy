package id.ac.itb.lumen.persistence

import org.springframework.data.neo4j.annotation.GraphId
import org.springframework.data.neo4j.annotation.NodeEntity

/**
 * Created by Budhi on 21/01/2015.
 */
@NodeEntity
class Person {
    @GraphId
    Long id
    String uri
    String label
}

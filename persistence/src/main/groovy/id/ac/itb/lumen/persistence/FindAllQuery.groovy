package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import groovy.transform.CompileStatic

/**
 * Find all entities of particular class.
 * Created by Budhi on 22/01/2015.
 */
@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type", defaultImpl=FindAllQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name="FindAllQuery", value=FindAllQuery.class))
class FindAllQuery {
    /**
     * Class URI of the entities to be queried, for example
     * {@code http://yago-knowledge.org/resource/wordnet_person_100007846}
     */
    String classUri
}

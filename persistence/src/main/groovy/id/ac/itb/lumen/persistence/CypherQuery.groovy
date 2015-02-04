package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import groovy.transform.CompileStatic

/**
 * Executes a Neo4j Cypher query.
 * Created by Budhi on 22/01/2015.
 * @todo should be {@link org.springframework.data.domain.Pageable}
 */
@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type", defaultImpl=CypherQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name="CypherQuery", value=CypherQuery.class))
class CypherQuery {
    /**
     * Cypher query, e.g.:
     *
     * <pre>
     * MATCH (n: Resource) -[:rdf_type]-> (:Resource {href: 'yago:wordnet_person_100007846'}) RETURN n LIMIT 25;
     * </pre>
     */
    String query
    /**
     * Query parameters (if any).
     */
    Map<String, Object> parameters = [:]
    /**
     * From hydra:itemsPerPage
     */
    Integer itemsPerPage
}

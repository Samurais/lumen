package id.ac.itb.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.data.domain.Pageable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes a Neo4j Cypher query.
 * Created by Budhi on 22/01/2015.
 *
 * @todo should be {@link Pageable}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = CypherQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "CypherQuery", value = CypherQuery.class))
public class CypherQuery {
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Integer getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(Integer itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * Cypher query, e.g.:
     * <p>
     * <pre>
     * MATCH (n: Resource) -[:rdf_type]-> (:Resource {href: 'yago:wordnet_person_100007846'}) RETURN n LIMIT 25;
     * </pre>
     */
    private String query;
    /**
     * Query parameters (if any).
     */
    private Map<String, Object> parameters = new LinkedHashMap<String, Object>();
    /**
     * From hydra:itemsPerPage
     */
    private Integer itemsPerPage;
}

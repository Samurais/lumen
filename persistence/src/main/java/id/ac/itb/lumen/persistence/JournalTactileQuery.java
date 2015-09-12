package id.ac.itb.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import groovy.transform.CompileStatic;
import org.springframework.data.domain.Pageable;

/**
 * Executes a Neo4j Cypher query.
 * Created by Budhi on 22/01/2015.
 *
 * @todo should be {@link Pageable}
 */
@CompileStatic
@JsonInclude(JsonInclude.com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = JournalTactileQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "JournalTactileQuery", value = JournalTactileQuery.class))
public class JournalTactileQuery {
    public String getMaxDateCreated() {
        return maxDateCreated;
    }

    public void setMaxDateCreated(String maxDateCreated) {
        this.maxDateCreated = maxDateCreated;
    }

    public Integer getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(Integer itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    private String maxDateCreated;
    /**
     * From hydra:itemsPerPage
     */
    private Integer itemsPerPage;
}

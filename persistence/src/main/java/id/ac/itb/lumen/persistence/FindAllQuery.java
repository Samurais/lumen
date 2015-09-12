package id.ac.itb.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.data.domain.Pageable;

/**
 * Find all entities of particular class.
 * Created by Budhi on 22/01/2015.
 *
 * @todo should be {@link Pageable}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = FindAllQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "FindAllQuery", value = FindAllQuery.class))
public class FindAllQuery {
    public String getClassRef() {
        return classRef;
    }

    public void setClassRef(String classRef) {
        this.classRef = classRef;
    }

    public Integer getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(Integer itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    /**
     * Class URI or QName of the entities to be queried, for example
     * {@code http://yago-knowledge.org/resource/wordnet_person_100007846}
     * or simply {@code yago:wordnet_person_100007846}
     */
    private String classRef;
    /**
     * From hydra:itemsPerPage
     */
    private Integer itemsPerPage;
}

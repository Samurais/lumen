package id.ac.itb.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * Provide all entities of particular class.
 * * Created by Budhi on 22/01/2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = Resources.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "Resources", value = Resources.class))
public class Resources<T> {
    public Resources(List<T> newContent) {
        this.content = newContent;
    }

    @Override
    public String toString() {
        return "Resources{" + "content=" + content + "}";
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    /**
     * List of the entities to be provided, for example
     * List of People or List of Cities
     */
    private List<T> content;
}

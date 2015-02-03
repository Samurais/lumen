package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import groovy.transform.CompileStatic

/**
 * Provide all entities of particular class.
 * * Created by Budhi on 22/01/2015.
 */
@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type", defaultImpl=Resources.class)
@JsonSubTypes(@JsonSubTypes.Type(name="Resources", value=Resources.class))
class Resources<T> {
    /**
     * List of the entities to be provided, for example
     * List of People or List of Cities 
      */
    List<T> content

    Resources(List<T> newContent) {
        this.content = newContent
    }

    @Override
    public String toString() {
        return "Resources{" +
                "content=" + content +
                '}';
    }
}

package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import groovy.transform.CompileStatic

/**
 * Created on 2/4/15.
 */
@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type", defaultImpl=FindAllQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name="ResultCell", value=FindAllQuery.class))
class ResultCell {
    String name
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME)
    Object value

    ResultCell(String name, Object value) {
        this.name = name
        this.value = value
    }
}

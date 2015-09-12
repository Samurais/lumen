package id.ac.itb.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import groovy.transform.CompileStatic;

/**
 * Created on 2/4/15.
 */
@CompileStatic
@JsonInclude(JsonInclude.com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = FindAllQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "ResultCell", value = FindAllQuery.class))
public class ResultCell {
    public ResultCell(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    private String name;
    @JsonTypeInfo(use = JsonTypeInfo.com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME)
    private Object value;
}

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
@JsonSubTypes(@JsonSubTypes.Type(name="ResultRow", value=FindAllQuery.class))
class ResultRow {

    List<ResultCell> cells = []

    ResultRow(List<ResultCell> cells) {
        this.cells = cells
    }
}

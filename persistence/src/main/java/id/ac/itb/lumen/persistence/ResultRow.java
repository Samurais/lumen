package id.ac.itb.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import groovy.transform.CompileStatic;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2/4/15.
 */
@CompileStatic
@JsonInclude(JsonInclude.com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = FindAllQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "ResultRow", value = FindAllQuery.class))
public class ResultRow {
    public ResultRow(List<ResultCell> cells) {
        this.cells = cells;
    }

    public ArrayList<ResultCell> getCells() {
        return cells;
    }

    public void setCells(List<ResultCell> cells) {
        this.cells = cells;
    }

    private List<ResultCell> cells = new ArrayList<ResultCell>();
}

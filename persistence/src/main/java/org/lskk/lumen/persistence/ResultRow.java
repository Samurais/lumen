package org.lskk.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2/4/15.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = FindAllQuery.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "ResultRow", value = FindAllQuery.class))
public class ResultRow {
    public ResultRow(List<ResultCell> cells) {
        this.cells = cells;
    }

    public List<ResultCell> getCells() {
        return cells;
    }

    public void setCells(List<ResultCell> cells) {
        this.cells = cells;
    }

    private List<ResultCell> cells = new ArrayList<ResultCell>();
}

package org.lskk.lumen.reasoner.ux;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Just like Wicket components, NuiComponents can be persisted to jBPM
 * Created by ceefour on 21/02/2016.
 */
public abstract class NuiComponent implements Serializable {

    private final String id;
    private Object model;
    private final List<NuiComponent> children = new ArrayList<>();

    public NuiComponent(String id) {
        this.id = id;
    }

    public NuiComponent(String id, Object model) {
        this.id = id;
        this.model = model;
    }

    public String getId() {
        return id;
    }

    public Object getModel() {
        return model;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    public void add(NuiComponent... children) {
        this.children.addAll(Arrays.asList(children));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", model=" + model +
                ", children=" + children +
                '}';
    }
}

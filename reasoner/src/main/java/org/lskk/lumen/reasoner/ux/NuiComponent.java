package org.lskk.lumen.reasoner.ux;

import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.reasoner.ReasonerException;

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
    protected final List<NuiComponent> children = new ArrayList<>();

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

    public <T> T getModelAs(Class<T> clazz) {
        return (T) model;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    public void add(NuiComponent... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * @see org.apache.wicket.Component#get(String)
     * @param path
     * @return
     */
    public NuiComponent get(String path) {
        final String topId = StringUtils.substringBefore(path, ":");
        final NuiComponent top = children.stream().filter(it -> topId.equals(it.getId())).findAny()
                .orElseThrow(() -> new ReasonerException(String.format("Cannot find component '%s' in '%s' for path '%s'", topId, getId(), path)));
        final String restPath = StringUtils.substringAfter(path, ":");
        if (!"".equals(restPath)) {
            return top.get(restPath);
        } else {
            return top;
        }
    }

    /**
     * Renders the NUI component suitable for eSpeak or Azure Speech.
     * @return
     */
    public String renderSsml() {
        throw new UnsupportedOperationException("NUI component " + getClass().getName() + " does not (yet) support SSML.");
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

package org.lskk.lumen.reasoner.ux;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.reasoner.ReasonerException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Just like Wicket components, NuiComponents can be persisted to jBPM
 * Created by ceefour on 21/02/2016.
 */
public abstract class NuiComponent implements Serializable {

    private final String id;
    private Object model;
    protected final List<NuiComponent> children = new ArrayList<>();
    private NuiComponent parent;

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
        for (final NuiComponent child : children) {
            child.setParent(this);
            this.children.add(child);
        }
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

    public Locale getLocale() {
        if (parent != null) {
            return Preconditions.checkNotNull(parent.getLocale(),
                    "%s's parent %s should return proper getLocale()", getId(), parent.getId());
        } else {
            throw new UnsupportedOperationException("Topmost component should support locale.");
        }
    }

    public NuiComponent getParent() {
        return parent;
    }

    protected void setParent(NuiComponent parent) {
        this.parent = parent;
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

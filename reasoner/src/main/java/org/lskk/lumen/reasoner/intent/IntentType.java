package org.lskk.lumen.reasoner.intent;

import org.lskk.lumen.reasoner.ReasonerException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The registration for intents, which optionally maps intent IDs to a specific {@link Intent} subclass, and declares slots to {@link EntityType}s.
 * At runtime, instances of {@link Intent} subclasses are created.
 * Created by ceefour on 17/02/2016.
 */
public class IntentType implements Serializable {

    private String id;
    private String className;
    private List<Slot> slots = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * If {@code null}, the intent data will just use {@link Intent},
     * meaning a very simple intent type that does not require any dependency.
     * @return
     */
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<Slot> getSlots() {
        return slots;
    }

    /**
     * If {@link #getClassName()} is {@code null}, will return {@link Intent}.
     * @param <T>
     * @return
     */
    public <T extends Intent> Class<T> getIntentClass() {
        try {
            return getClassName() != null ? (Class<T>) IntentType.class.forName(getClassName()) : (Class<T>) Intent.class;
        } catch (Exception e) {
            throw new ReasonerException(e, "Cannot load intent %s's class %s", getId(), getClassName());
        }
    }

}

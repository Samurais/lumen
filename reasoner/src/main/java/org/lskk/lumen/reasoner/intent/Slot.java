package org.lskk.lumen.reasoner.intent;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link IntentType}'s dependency to {@link EntityType}.
 * Created by ceefour on 17/02/2016.
 * @see <a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/defining-the-voice-interface#The Intent Schema">Amazon Alexa Skills Kit - Intent Schema</a></a>
 */
public class Slot implements Serializable {
    private String id;
    private boolean required;
    private SlotKind kind;
    private EntityType entityType;
    private Set<String> literals = new HashSet<>();
    private Set<String> thingTypes = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public SlotKind getKind() {
        return kind;
    }

    public void setKind(SlotKind kind) {
        this.kind = kind;
    }

    /**
     * A shortcut for creating {@link LiteralEntityType} then linking to it.
     * @return
     */
    public Set<String> getLiterals() {
        return literals;
    }

    /**
     * Shortcut for creating {@link ThingEntityType} then linking to it.
     * @return
     */
    public Set<String> getThingTypes() {
        return thingTypes;
    }
}

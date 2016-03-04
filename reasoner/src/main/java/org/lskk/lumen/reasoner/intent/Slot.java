package org.lskk.lumen.reasoner.intent;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * {@link IntentType}'s dependency to {@link EntityType}.
 * Created by ceefour on 17/02/2016.
 * @see <a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/defining-the-voice-interface#The Intent Schema">Amazon Alexa Skills Kit - Intent Schema</a></a>
 */
public class Slot implements Serializable {

    public enum Direction {
        IN,
        OUT
    };

    private String id;
    private boolean required;
    private SlotKind kind;
    private EntityType entityType;
    private Set<String> literals = new HashSet<>();
    private Set<String> thingTypes = new HashSet<>();
    private Object last;

    private transient Queue<Object> inQueue;
    private transient Queue<Object> outQueue;

    @PostConstruct
    public void initialize(Direction direction) {
        if (Direction.IN == direction) {
            inQueue = new ArrayDeque<>();
        } else {
            outQueue = new ArrayDeque<>();
        }
    }

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

    /**
     * Returns the last non-null value polled from the slot.
     * @return
     */
    public <T> T getLast() {
        return (T) last;
    }

    public <T> T poll() {
        final Object obj = inQueue.poll();
        if (null != obj) {
            this.last = obj;
        }
        return (T) obj;
    }

    /**
     * Add an packet to the end of the queue.
     * In-slots only.
     * @param packet
     * @return
     */
    public <T> T add(T packet) {
        inQueue.add(packet);
        return packet;
    }

    /**
     * Sends an information packet to the queue. Out-slots only.
     * @param packet
     * @return
     */
    public <T> T send(T packet) {
        outQueue.add(packet);
        return packet;
    }

    public Queue<Object> getOutQueue() {
        return outQueue;
    }

}

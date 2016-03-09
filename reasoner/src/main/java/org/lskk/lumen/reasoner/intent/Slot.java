package org.lskk.lumen.reasoner.intent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.lskk.lumen.core.LumenType;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.*;

/**
 * {@link IntentType}'s dependency to {@link EntityType}.
 * Created by ceefour on 17/02/2016.
 * @see <a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/defining-the-voice-interface#The Intent Schema">Amazon Alexa Skills Kit - Intent Schema</a></a>
 */
public class Slot implements Serializable {

    public static final String CONTROL = "control";
    public static final String COMPLETED = "completed";

    public enum Direction {
        IN,
        OUT
    }

    private String id;
    private boolean required;
    private SlotKind kind;
    private EntityType entityType;
    private Set<String> literals = new HashSet<>();
    private Set<String> thingTypes = new HashSet<>();
    private Object last;
    private Serializable initial;

    private transient Queue<Object> inQueue;
    private transient Queue<Object> outQueue;

    @PostConstruct
    public void initialize(Direction direction) {
        if (Direction.IN == direction) {
            inQueue = new ArrayDeque<>();
            if (null != getInitial()) {
                add(initial);
            }
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
     * Only used for in-slots, and only usable for primitive types, e.g. {@link String}, {@link Number},
     * and {@link Boolean}.
     * @return
     */
    public Object getInitial() {
        return initial;
    }

    public void setInitial(JsonNode json) {
        switch (thingTypes.iterator().next()) {
            case "xsd:string":
                this.initial = json.asText();
                break;
            case "xsd:float":
                this.initial = (float) json.asDouble();
                break;
            case "xsd:date":
                this.initial = new LocalDate(json.asText());
                break;
            case "xsd:time":
                this.initial = new LocalTime(json.asText());
                break;
            case "yago:TimeZone":
                this.initial = DateTimeZone.forID(json.asText());
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupported slot '%s' type %s for initial '%s'",
                        getId(), thingTypes, json));
        }
    }

    /**
     * A shortcut for creating {@link LiteralEntityType} then linking to it.
     * @return
     */
    public Set<String> getLiterals() {
        return literals;
    }

    /**
     * Shortcut for creating {@link ThingEntityType} then linking to it,
     * e.g. {@code xs:date}, {@code yago:wordnet_person_100007846}.
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

    @JsonIgnore
    public Queue<Object> getOutQueue() {
        return outQueue;
    }

    /**
     * Returns whether there's a packet waiting in the in-queue.
     * @return
     */
    public boolean hasNext() {
        Preconditions.checkNotNull(inQueue, "inQueue for in-slot %s %s is null, have you called initialize()?", getId(), getThingTypes());
        return !inQueue.isEmpty();
    }

}

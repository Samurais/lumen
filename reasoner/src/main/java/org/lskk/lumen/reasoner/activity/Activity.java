package org.lskk.lumen.reasoner.activity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.intent.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.*;

/**
 * Base class for natural interaction pattern that is readily usable.
 * A task instance is rich domain object and specifies both behavior and state, is {@link java.io.Serializable} and tied to one process.
 * Why we didn't separate this? Because separating violates Domain-Driven principle.
 *      Wicket also separates between Component.java and markup Component.html, but a Component
 *      is a Component, there is not ComponentDef and ComponentExecution.
 *      If Spring injection is the issue, we have two options:
 *      1. Spring factory method injection, doesn't work with {@link Serializable}
 *      2. Each method receives the required service bean(s), doesn't work with {@link Serializable}
 *      3. {@link org.springframework.beans.factory.annotation.Configurable} + LTW. Works with {@link Serializable}.
 *      Either way, there's no {@code TaskExec} DTO as separate DTO.
 *
 * Created by ceefour on 17/02/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Activity implements Serializable {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private String id;
    private String description;
    private ActivityState state = ActivityState.PENDING;
    private InteractionSession parent;
    private Boolean enabled;
    private List<Slot> inSlots = new ArrayList<>();
    private Boolean autoPoll;

    /**
     * Inferred from the JSON filename, e.g. {@code promptBirthDate.PromptTask.json} means the ID
     * is {@code promptBirthdate}.
     * @return
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Helpful description for skill creator.
     * @return
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * If {@link InteractionSession#getActiveTask()} is this one then true, else false.
     * @return
     */
    public boolean isActive() {
        return ActivityState.ACTIVE == state;
    }

    /**
     * You must override this to handle user's input.
     * @param communicateAction
     * @param session
     */
    public void receiveUtterance(CommunicateAction communicateAction, InteractionSession session) {
    }

    public ActivityState getState() {
        return state;
    }

    public void setState(ActivityState state) {
        this.state = state;
    }

    /**
     * Checks whether an utterance matched the defined patterns for this {@link Activity}.
     * IMPORTANT: This method must be strictly stateless.
     *
     * @param locale
     * @param utterance
     * @param scope     If {@link Activity} is not active or is used as a {@link org.lskk.lumen.reasoner.skill.Skill}'s intent,
     *                  use {@link org.lskk.lumen.reasoner.activity.UtterancePattern.Scope#GLOBAL}.
     *                  If {@link Activity} is active, use {@link org.lskk.lumen.reasoner.activity.UtterancePattern.Scope#ANY}.
     * @return
     */
    public List<UtterancePattern> matchUtterance(Locale locale, String utterance, UtterancePattern.Scope scope) {
        return ImmutableList.of();
    }

    /**
     * Override this to handle lifecycle state transitions.
     * @param previous
     * @param current
     * @param locale Specific {@link Locale} that was active during the state change, it's always one of {@link InteractionSession#getActiveLocales()}.
     * @param session
     */
    public void onStateChanged(ActivityState previous, ActivityState current, Locale locale, InteractionSession session) throws Exception {
        if (ActivityState.ACTIVE == current) {
            if (getAutoPoll()) {
                getInSlots().forEach(slot -> {
                    final Object value = slot.poll();
                    Preconditions.checkState(!slot.isRequired() || null != value,
                            "Slot %s.%s [%s] is required but a packet was not received",
                            getPath(), slot.getId(), slot.getThingTypes());
                });
            }
        }
    }

    public InteractionSession getParent() {
        return parent;
    }

    public void setParent(InteractionSession parent) {
        this.parent = parent;
    }

    public String getPath() {
        return (parent != null ? parent.getId() + "/" : "") + getId();
    }

    /**
     * At runtime after {@link #initialize()} is called, this will be default to true.
     * @return
     */
    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @PostConstruct
    public void initialize() {
        this.enabled = Optional.ofNullable(this.enabled).orElse(true);
        this.autoPoll = Optional.ofNullable(this.autoPoll).orElse(false);
        inSlots.forEach(it -> it.initialize(Slot.Direction.IN));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + getId();
    }

    public List<Slot> getInSlots() {
        return inSlots;
    }

    /**
     * If {@code true}, all in-slots will be polled automatically before the {@link org.lskk.lumen.reasoner.activity.Activity}
     * transitions to {@link org.lskk.lumen.reasoner.activity.ActivityState#ACTIVE}, so {@link Slot#getLast()}
     * can be directly called. This makes the programming model much simpler, like synchronous, but less flexible.
     * @return
     */
    public Boolean getAutoPoll() {
        return autoPoll;
    }

    public void setAutoPoll(Boolean autoPoll) {
        this.autoPoll = autoPoll;
    }
}

package org.lskk.lumen.reasoner.activity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.intent.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private String name;
    private String description;
    private ActivityState state = ActivityState.PENDING;
    private Activity parent;
    private Boolean enabled;
    private List<Slot> inSlots = new ArrayList<>();
    private Boolean autoPoll;
    private List<Slot> outSlots = new ArrayList<>();
    private List<Activity> activities = new ArrayList<>();
    private Boolean autoStart;

    public Activity() {
    }

    public Activity(String id) {
        this.id = id;
    }

    /**
     * After this {@link Activity} is completed, the assigned out-slots will be polled
     * and sent to be in-slots of the sink activity.
     *
     * <p>Example:</p>
     *
     * <pre>
     *     "outSlots": [
     *      {"id": "chapter", "thingTypes": ["xsd:string"]},
     *      {"id": "verse", "thingTypes": ["xsd:integer"]}
     *     ]
     </pre>
     * @return
     */
    public List<Slot> getOutSlots() {
        return outSlots;
    }

    public Slot getOutSlot(String slotId) {
        return outSlots.stream().filter(it -> slotId.equals(it.getId())).findAny()
                .orElseThrow(() -> new ReasonerException(String.format("Cannot find out-slot %s.%s, %s available out-slots are: %s",
                        getPath(), slotId, outSlots.size(), outSlots.stream().map(Slot::getId).collect(Collectors.toList()))));
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
     * If {@link InteractionSession#getFocusedTask()} is this one then true, else false.
     * @return
     */
    public boolean isActive() {
        return ActivityState.ACTIVE == state;
    }

    /**
     * An auto-start activity will be {@link InteractionSession#activate(Activity, Locale)}-d automatically
     * when its parent is activated. A shortcut to creating a {@code Start} activity and connecting its control flow
     * to this activity. Note: auto-start has no effect if this activity has any required in-slots.
     * @return
     */
    public Boolean getAutoStart() {
        return autoStart;
    }

    public void setAutoStart(Boolean autoStart) {
        this.autoStart = autoStart;
    }

    /**
     * You must override this to handle user's input.
     * @param communicateAction
     * @param session
     * @param focusedTask
     */
    public void receiveUtterance(CommunicateAction communicateAction, InteractionSession session, Task focusedTask) {
        activities.stream().filter(it -> ActivityState.ACTIVE == it.getState()).forEach(activity -> {
            log.trace("Executing receiveUtterance for '{}': {}", activity.getPath(), communicateAction);
            activity.receiveUtterance(communicateAction, session, focusedTask);
        });
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

    public Activity getParent() {
        return parent;
    }

    public void setParent(Activity parent) {
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
        this.autoStart = Optional.ofNullable(this.autoStart).orElse(false);
        inSlots.forEach(it -> it.initialize(Slot.Direction.IN));
        outSlots.forEach(it -> it.initialize(Slot.Direction.OUT));

        activities.forEach(Activity::initialize);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + getId();
    }

    /**
     * <p>Example:</p>
     *
     * <pre>
     * "autoPoll": true,
     * "inSlots": [
     *     {"id": "measure", "thingTypes": ["yago:yagoQuantity"], "required": true},
     *     {"id": "unit", "thingTypes": ["yago:wordnet_unit_of_measurement_113583724"], "required": true}
     * ],
     * </pre>
     * @return
     */
    public List<Slot> getInSlots() {
        return inSlots;
    }

    public Slot getInSlot(String slotId) {
        return inSlots.stream().filter(it -> slotId.equals(it.getId())).findAny()
                .orElseThrow(() -> new ReasonerException(String.format("Cannot find in-slot %s.%s, %s available in-slots are: %s",
                        getPath(), slotId, inSlots.size(), inSlots.stream().map(Slot::getId).collect(Collectors.toList()))));
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

    public List<Activity> getActivities() {
        return activities;
    }

    public Activity add(Activity activity) {
        activity.setParent(this);
        activities.add(activity);
        return activity;
    }

    /**
     * An activity is ready if it can be transitioned into {@link ActivityState#ACTIVE},
     * meaning that all required {@link #getInSlots()}s has packets waiting in the queue.
     * @return
     */
    public boolean isReady() {
        return getInSlots().stream().filter(Slot::isRequired)
                .allMatch(slot -> slot.hasNext());
    }

    /**
     * Do necessary actions in this state, such as polling in-slots.
     * @param session
     * @param locale
     */
    public void pollActions(InteractionSession session, Locale locale) {
        activities.forEach(act -> act.pollActions(session, locale));
    }

    public <T extends Activity> T get(String relativePath) {
        return (T) activities.stream().filter(it -> relativePath.equals(it.getId())).findAny()
                .orElseThrow(() -> new ReasonerException(String.format("Cannot find Activity '%s' in '%s'", relativePath, getPath())));
    }

    /**
     * Visits all enabled descendants and return first non-null value.
     * @param visitor
     * @param <R>
     * @return
     */
    public <R> R visitFirst(Function<Activity, R> visitor) {
        final R result = visitor.apply(this);
        if (null != result) {
            return result;
        } else {
            return activities.stream().filter(Activity::getEnabled).map(it -> it.visitFirst(visitor))
                    .filter(Objects::nonNull).findFirst().orElse(null);
        }
    }
}

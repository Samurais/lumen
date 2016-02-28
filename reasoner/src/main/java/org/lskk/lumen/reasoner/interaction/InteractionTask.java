package org.lskk.lumen.reasoner.interaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.persistence.neo4j.Literal;
import org.lskk.lumen.persistence.neo4j.ThingLabel;
import org.lskk.lumen.reasoner.expression.Proposition;

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
public abstract class InteractionTask implements Serializable {

    private InteractionTaskState state = InteractionTaskState.PENDING;
    private List<UtterancePattern> matchedUtterancePatterns = new ArrayList<>();
    private Queue<ThingLabel> labelsToAssert = new ArrayDeque<>();
    private Queue<Literal> literalsToAssert = new ArrayDeque<>();
    private Queue<Proposition> pendingPropositions = new ArrayDeque<>();
    private Queue<CommunicateAction> pendingCommunicateActions = new ArrayDeque<>();

    /**
     * If {@link InteractionSession#getActiveTask()} is this one then true, else false.
     * @return
     */
    public boolean isActive() {
        return InteractionTaskState.ACTIVE == state;
    }

    /**
     * Used by {@link PromptTask}.
     * @return
     */
    public List<UtterancePattern> getMatchedUtterancePatterns() {
        return matchedUtterancePatterns;
    }

    /**
     * Given TaskExec state, returns the proposition that Lumen wants to express
     * to the user (if any).
     * @param locale
     * @return
     */
    public Optional<QuestionTemplate> getProposition(Locale locale) {
        throw new UnsupportedOperationException();
    }

    /**
     * You must override this to handle user's input.
     * @param communicateAction
     */
    public void receiveUtterance(CommunicateAction communicateAction) {
    }

    /**
     * Assertable {@link ThingLabel}s, they will be {@link Queue#poll()}-ed by {@link InteractionSession}
     * and asserted to Persistence using {@link org.lskk.lumen.persistence.service.FactService#assertLabel(String, String, String, String, float[], DateTime, String)}.
     * @return
     */
    public Queue<ThingLabel> getLabelsToAssert() {
        return labelsToAssert;
    }

    /**
     * Assertable {@link Literal}s, they will be {@link Queue#poll()}-ed by {@link InteractionSession}
     * and asserted to Persistence using {@link org.lskk.lumen.persistence.service.FactService#assertPropertyToLiteral(String, String, String, Object, float[], DateTime, String)}.
     * @return
     */
    public Queue<Literal> getLiteralsToAssert() {
        return literalsToAssert;
    }

    public InteractionTaskState getState() {
        return state;
    }

    public void setState(InteractionTaskState state) {
        this.state = state;
    }

    /**
     * Override this to handle lifecycle state transitions.
     * @param previous
     * @param current
     * @param locale Specific {@link Locale} that was active during the state change, it's always one of {@link InteractionSession#getActiveLocales()}.
     * @param session
     */
    public void onStateChanged(InteractionTaskState previous, InteractionTaskState current, Locale locale, InteractionSession session) {

    }

    /**
     * Will be {@link Queue#poll()}-ed by {@link InteractionSession} to {@link org.lskk.lumen.reasoner.ux.Channel#express(String, Proposition, boolean, Object)}
     * back to the user.
     * @return
     */
    public Queue<Proposition> getPendingPropositions() {
        return pendingPropositions;
    }

    /**
     * Will be {@link Queue#poll()}-ed by {@link InteractionSession} to {@link org.lskk.lumen.reasoner.ux.Channel#express(String, CommunicateAction, Object)}
     * back to the user.
     * @return
     */
    public Queue<CommunicateAction> getPendingCommunicateActions() {
        return pendingCommunicateActions;
    }
}

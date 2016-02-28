package org.lskk.lumen.reasoner.interaction;

import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.persistence.neo4j.Literal;
import org.lskk.lumen.persistence.neo4j.ThingLabel;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.ux.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;

/**
 * Soon this will be replaced by {@link org.kie.internal.runtime.StatefulKnowledgeSession},
 * but for now I'll use this to prototype the intended behavior.
 *
 * <p>Serializable, state is stored. So either use Spring to create this prototype bean using {@link javax.inject.Provider}
 * or pass service beans in methods.</p>
 * <p>
 * Lifecycle:
 * (new) -open-> Running -close-> (gone)
 * Running -poke-> Running
 * Running -suspend-> Suspended
 * Suspended -resume-> Running
 * <p>
 * Created by ceefour on 26/02/2016.
 */
@Component
@Scope("prototype")
public class InteractionSession implements Serializable, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(InteractionSession.class);

    @Inject
    private FactService factService;

    private List<Locale> activeLocales = new ArrayList<>();
    private Locale lastLocale;
    private InteractionTask activeTask;
    private List<InteractionTask> tasks = new ArrayList<>();
    private Queue<InteractionTask> pendingActivations = new ArrayDeque<>();

    public void open() {
        Preconditions.checkState(!activeLocales.isEmpty(),
                "Requires at least one active locale");
        lastLocale = activeLocales.get(0);
        update(null);
    }

    @PreDestroy
    public void close() {

    }

    /**
     * Currently active task will be able to match {@link UtterancePattern}
     * with {@link org.lskk.lumen.reasoner.interaction.UtterancePattern.Scope#LOCAL}.
     * FIXME: this shouldn't be "InteractionTask" behavior but a stateful DTO, i.e. TaskExec
     *
     * @return
     */
    public InteractionTask getActiveTask() {
        return activeTask;
    }

    public void activate(InteractionTask nextTask, Locale locale) {
        if (null != this.activeTask && InteractionTaskState.ACTIVE == this.activeTask.getState()) {
            log.debug("Deactivating {} for {} ...", this.activeTask, nextTask);
            final InteractionTaskState previous = this.activeTask.getState();
            this.activeTask.setState(InteractionTaskState.PENDING);
            this.activeTask.onStateChanged(previous, this.activeTask.getState(), locale, this);
            pollTaskActions(this.activeTask);
        }
        this.activeTask = nextTask;
        if (null != this.activeTask && InteractionTaskState.PENDING == this.activeTask.getState()) {
            final InteractionTaskState previous = this.activeTask.getState();
            log.debug("Activating from {}: {} ...", previous, nextTask);
            this.activeTask.setState(InteractionTaskState.ACTIVE);
            this.activeTask.onStateChanged(previous, this.activeTask.getState(), locale, this);
            pollTaskActions(this.activeTask);
        }
    }

    /**
     * Background tasks can only match {@link UtterancePattern}
     * with scope {@link org.lskk.lumen.reasoner.interaction.UtterancePattern.Scope#GLOBAL}.
     * FIXME: this shouldn't be "InteractionTask" behavior but a stateful DTO, i.e. TaskExec
     *
     * @return
     */
    public List<InteractionTask> getTasks() {
        return tasks;
    }

    public List<Locale> getActiveLocales() {
        return activeLocales;
    }

    /**
     * If the task (usually {@link #getActiveTask()} has pending actions, poll them:
     * <p>
     * <ol>
     * <li>{@link InteractionTask#getLabelsToAssert()}</li>
     * <li>{@link InteractionTask#getLiteralsToAssert()}</li>
     * <li>{@link InteractionTask#getPendingCommunicateActions()}</li>
     * <li>{@link InteractionTask#getPendingPropositions()}</li>
     * </ol>
     *
     * @param task
     */
    protected void pollTaskActions(InteractionTask task) {
        while (!task.getLabelsToAssert().isEmpty()) {
            final ThingLabel label = task.getLabelsToAssert().poll();
            if (label.getConfidence() >= 0.9f) {
                log.info("Asserting {}", label);
                factService.assertLabel(label.getThingQName(), label.getPropertyQName(),
                        label.getValue(), label.getInLanguage(),
                        new float[]{1f, label.getConfidence(), 0}, new DateTime(), null);
            } else {
                log.info("Skipped low confidence {}", label);
            }
        }
        while (!task.getLiteralsToAssert().isEmpty()) {
            final Literal literal = task.getLiteralsToAssert().poll();
            if (literal.getConfidence() >= 0.9f) {
                log.info("Asserting {}", literal);
                factService.assertPropertyToLiteral(literal.getSubject().getNn(), literal.getPredicate().getNn(),
                        literal.getType(), literal.getValue(),
                        new float[]{1f, literal.getConfidence(), 0}, new DateTime(), null);
            } else {
                log.info("Skipped low confidence {}", literal);
            }
        }
    }

    // TODO: should parameters replaced by CommunicateAction?
    public void receiveUtterance(Locale locale, String text, FactService factService) {
        lastLocale = locale;
        final CommunicateAction communicateAction = new CommunicateAction(locale, text, null);
        if (null != activeTask) {
            final InteractionTask executing = activeTask;
            log.info("Executing receiveUtterance for {}: {}", executing, communicateAction);
            executing.receiveUtterance(communicateAction, this);
            pollTaskActions(executing);
        } else {
            log.warn("No active task to receiveUtterance for {}", communicateAction);
        }
    }

    /**
     * Call this to fire pending scheduled tasks.
     * @param channel
     */
    public void update(Channel<?> channel) {
        final String avatarId = null;
        for (final InteractionTask task : tasks) {
            while (true) {
                final CommunicateAction pendingCommunicateAction = task.getPendingCommunicateActions().poll();
                if (null == pendingCommunicateAction) {
                    break;
                }
                channel.express(avatarId, pendingCommunicateAction, null);
            }
        }

        final InteractionTask nextTask = pendingActivations.poll();
        if (null != nextTask) {
            activate(nextTask, getLastLocale());
        }
    }

    public Locale getLastLocale() {
        return lastLocale;
    }

    public Queue<InteractionTask> getPendingActivations() {
        return pendingActivations;
    }

    /**
     * Mark specified task as {@link InteractionTaskState#COMPLETED}.
     *
     * @param task
     */
    public void complete(InteractionTask task, Locale locale) {
        Preconditions.checkArgument(InteractionTaskState.ACTIVE == task.getState(),
                "Can only complete an ACTIVE task, but got %s", task);
        final InteractionTaskState previous = task.getState();
        task.setState(InteractionTaskState.COMPLETED);
        if (task == this.activeTask) {
            this.activeTask = null;
        }
        task.onStateChanged(previous, task.getState(), locale, this);
        pollTaskActions(task);
    }

    public void schedule(InteractionTask task) {
        getPendingActivations().add(task);
    }
}

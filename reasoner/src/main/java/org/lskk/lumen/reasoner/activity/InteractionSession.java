package org.lskk.lumen.reasoner.activity;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.core.IConfidence;
import org.lskk.lumen.persistence.neo4j.Literal;
import org.lskk.lumen.persistence.neo4j.ThingLabel;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.skill.Skill;
import org.lskk.lumen.reasoner.skill.SkillRepository;
import org.lskk.lumen.reasoner.skill.TaskRef;
import org.lskk.lumen.reasoner.ux.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Soon this will be replaced by {@link org.kie.internal.runtime.StatefulKnowledgeSession},
 * but for now I'll use this to prototype the intended behavior.
 * <p>
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
    /**
     * Minimum intent confidence before a session will materialize the skill.
     */
    public static final float INTENT_MIN_CONFIDENCE = 0.8f;
    /**
     * Minimum {@link ThingLabel} confidence before a session will assert it.
     */
    public static final float ASSERT_LABEL_MIN_CONFIDENCE = 0.8f;
    /**
     * Minimum {@link Literal} confidence before a session will assert it.
     */
    public static final float ASSERT_LITERAL_MIN_CONFIDENCE = 0.8f;
    private static final AtomicLong SEQ_ID = new AtomicLong(0);

    @Inject
    private FactService factService;
    @Inject
    private SkillRepository skillRepo;

    private long id = SEQ_ID.incrementAndGet();
    private List<Locale> activeLocales = new ArrayList<>();
    private Locale lastLocale;
    private Task activeTask;
    private List<Task> tasks = new ArrayList<>();
    private Queue<Task> pendingActivations = new ArrayDeque<>();

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
     * with {@link org.lskk.lumen.reasoner.activity.UtterancePattern.Scope#LOCAL}.
     * FIXME: this shouldn't be "InteractionTask" behavior but a stateful DTO, i.e. TaskExec
     *
     * @return
     */
    public Task getActiveTask() {
        return activeTask;
    }

    public long getId() {
        return id;
    }

    public void activate(Task nextTask, Locale locale) {
        if (null != this.activeTask && ActivityState.ACTIVE == this.activeTask.getState()) {
            log.debug("Deactivating {} for {} ...", this.activeTask, nextTask);
            final ActivityState previous = this.activeTask.getState();
            this.activeTask.setState(ActivityState.PENDING);
            try {
                this.activeTask.onStateChanged(previous, this.activeTask.getState(), locale, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            pollTaskActions(this.activeTask);
        }
        this.activeTask = nextTask;
        if (null != this.activeTask && ActivityState.PENDING == this.activeTask.getState()) {
            final ActivityState previous = this.activeTask.getState();
            log.debug("Activating from {}: {} ...", previous, nextTask);
            this.activeTask.setState(ActivityState.ACTIVE);
            try {
                this.activeTask.onStateChanged(previous, this.activeTask.getState(), locale, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            pollTaskActions(this.activeTask);
        }
    }

    /**
     * Background tasks can only match {@link UtterancePattern}
     * with scope {@link org.lskk.lumen.reasoner.activity.UtterancePattern.Scope#GLOBAL}.
     * You can't modify this! Use {@link #add(Activity)}.
     * FIXME: this shouldn't be "InteractionTask" behavior but a stateful DTO, i.e. TaskExec
     *
     * @return
     */
    public List<Activity> getTasks() {
        return ImmutableList.copyOf(tasks);
    }

    public List<Locale> getActiveLocales() {
        return activeLocales;
    }

    /**
     * If the task (usually {@link #getActiveTask()} has pending actions, poll them:
     * <p>
     * <ol>
     * <li>{@link Task#getLabelsToAssert()}</li>
     * <li>{@link Task#getLiteralsToAssert()}</li>
     * <li>{@link Task#getPendingCommunicateActions()}</li>
     * <li>{@link Task#getPendingPropositions()}</li>
     * </ol>
     *
     * @param task
     */
    protected void pollTaskActions(Task task) {
        while (!task.getLabelsToAssert().isEmpty()) {
            final ThingLabel label = task.getLabelsToAssert().poll();
            if (label.getConfidence() >= ASSERT_LABEL_MIN_CONFIDENCE) {
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
            if (literal.getConfidence() >= ASSERT_LITERAL_MIN_CONFIDENCE) {
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
    public void receiveUtterance(Locale locale, String text, FactService factService, TaskRepository taskRepo) {
        lastLocale = locale;
        final CommunicateAction communicateAction = new CommunicateAction(locale, text, null);

        // Check active task first
        boolean handled = false;
        if (null != activeTask) {
            final Task executing = activeTask;
            log.info("Executing receiveUtterance for {}: {}", executing, communicateAction);
            executing.receiveUtterance(communicateAction, this);
            pollTaskActions(executing);
            handled = true;
        } else {
            log.info("No active task to receiveUtterance for {} (will consult skills)", communicateAction);
            handled = false;
        }

        if (!handled) {
            final List<Skill> enabledSkills = skillRepo.getSkills().values().stream()
                    .filter(Skill::getEnabled).collect(Collectors.toList());
            final List<UtterancePattern> totalMatches = enabledSkills.stream().flatMap(skill -> {
                final List<UtterancePattern> skillMatches = skill.getIntents().stream().flatMap(intent -> {
                    final List<UtterancePattern> matches = intent.matchUtterance(locale, text, UtterancePattern.Scope.GLOBAL);
                    matches.forEach(match -> {
                        match.setIntent(intent);
                        match.setSkill(skill);
                    });
                    if (!matches.isEmpty()) {
                        log.debug("Skill '{}' intent '{}' returned {} matches for utterance '{}'@{}",
                                skill.getId(), intent.getId(), matches.size(), text, locale.toLanguageTag());
                    }
                    return matches.stream();
                }).collect(Collectors.toList());
                log.debug("Skill '{}' with {} intents {} returned {} matches for '{}'@{}",
                        skill.getId(), skill.getIntents().size(), skill.getIntents().stream().map(Activity::getId).toArray(), skillMatches.size(),
                        text, locale.toLanguageTag());
                return skillMatches.stream();
            }).sorted(new IConfidence.Comparator()).collect(Collectors.toList());
            log.info("Total {} matches for '{}'@{} from {} enabled skills:\n{}",
                    totalMatches.size(), text, locale.toLanguageTag(), enabledSkills.size(),
                    totalMatches.stream().limit(10)
                            .map(m -> String.format("* %s/%s: %s", m.getSkill().getId(), m.getIntent().getId(), m))
                            .collect(Collectors.joining("\n")));

            final Optional<UtterancePattern> best = totalMatches.stream().findFirst();
            if (best.isPresent() && best.get().getConfidence() >= INTENT_MIN_CONFIDENCE) {
                launchSkill(best.get(), taskRepo);
            } else if (best.isPresent()) {
                log.info("Best intent confidence is < {}, skipped {}/{}: {}", INTENT_MIN_CONFIDENCE,
                        best.get().getSkill().getId(), best.get().getIntent().getId());
            }
        }
    }

    /**
     * Create all {@link Activity}s of a {@link org.lskk.lumen.reasoner.skill.Skill} based
     * on matched {@link UtterancePattern}.
     *
     * @param utterancePattern
     */
    protected void launchSkill(UtterancePattern utterancePattern, TaskRepository taskRepo) {
        log.info("Launching {}/{} ...", utterancePattern.getSkill().getId(), utterancePattern.getIntent().getId());
        Task theIntent = null;
        for (final TaskRef taskRef : utterancePattern.getSkill().getTasks()) {
            final PromptTask promptTask = taskRepo.createPrompt(taskRef.getId());
            if (utterancePattern.getIntent().getId().equals(taskRef.getId())) {
                theIntent = promptTask;
            }
            promptTask.setId(utterancePattern.getSkill().getId() + "." + promptTask.getId());
            add(promptTask);
        }
        activate(theIntent, Locale.forLanguageTag(utterancePattern.getInLanguage()));
    }

    /**
     * Used by {@link #update(Channel)} to express all pending propositions.
     *
     * @param channel
     * @param avatarId
     */
    protected void expressAll(Channel<?> channel, String avatarId) {
        for (final Task task : tasks) {
            while (true) {
                final CommunicateAction pendingCommunicateAction = task.getPendingCommunicateActions().poll();
                if (null == pendingCommunicateAction) {
                    break;
                }
                channel.express(avatarId, pendingCommunicateAction, null);
            }
        }
    }

    /**
     * Call this to fire pending scheduled tasks.
     *
     * @param channel
     */
    public void update(Channel<?> channel) {
        final String avatarId = null;
        expressAll(channel, avatarId); // pre-activation express

        final Task nextTask = pendingActivations.poll();
        if (null != nextTask) {
            activate(nextTask, getLastLocale());
        }

        expressAll(channel, avatarId); // post-activation express
    }

    public Locale getLastLocale() {
        return lastLocale;
    }

    public Queue<Task> getPendingActivations() {
        return pendingActivations;
    }

    /**
     * Mark specified activity as {@link ActivityState#COMPLETED}.
     *
     * @param activity
     */
    public void complete(Activity activity, Locale locale) {
        Preconditions.checkArgument(ActivityState.ACTIVE == activity.getState(),
                "Can only complete an ACTIVE activity, but got %s", activity);
        log.debug("Completing from {}: {} ...", activity.getState(), activity);
        final ActivityState previous = activity.getState();
        activity.setState(ActivityState.COMPLETED);
        if (activity == this.activeTask) {
            this.activeTask = null;
        }
        try {
            activity.onStateChanged(previous, activity.getState(), locale, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (activity instanceof Task) {
            pollTaskActions((Task) activity);
        }
    }

    public void schedule(Task task) {
        getPendingActivations().add(task);
    }

    public Task getTask(String taskId) {
        return Preconditions.checkNotNull(tasks.stream().filter(it -> taskId.equals(it.getId())).findAny().orElse(null),
                "Cannot find task '%s' in session %s. %s available tasks are: %s",
                taskId, getId(), getTasks().size(), getTasks().stream().map(Activity::getId).toArray());
    }

    public Activity add(Activity activity) {
        activity.setParent(this);
        if (activity instanceof Task) {
            tasks.add((Task) activity);
        }
        return activity;
    }
}

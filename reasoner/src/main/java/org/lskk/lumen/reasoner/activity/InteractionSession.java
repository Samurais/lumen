package org.lskk.lumen.reasoner.activity;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.core.IConfidence;
import org.lskk.lumen.persistence.neo4j.Literal;
import org.lskk.lumen.persistence.neo4j.ThingLabel;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.skill.Skill;
import org.lskk.lumen.reasoner.skill.SkillRepository;
import org.lskk.lumen.reasoner.skill.TaskRef;
import org.lskk.lumen.reasoner.ux.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
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
    private Task focusedTask;
    private List<Activity> activities = new ArrayList<>();
    private Queue<Activity> pendingActivations = new ArrayDeque<>();

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
    public Task getFocusedTask() {
        return focusedTask;
    }

    public long getId() {
        return id;
    }

    public void activate(Activity activity, Locale locale) {
        Preconditions.checkNotNull(activity, "activity parameter must be provided");
        Preconditions.checkState(activity.isReady(), "Activity '%s' cannot be activated because it is not ready", activity.getPath());

        if (activity instanceof PromptTask) {
            if (null != this.focusedTask && ActivityState.ACTIVE == this.focusedTask.getState()) {
                log.debug("Defocusing {} for {} ...", this.focusedTask, activity);
                this.focusedTask = null;
            }
        }

        if (ActivityState.PENDING == activity.getState()) {
            final ActivityState previous = activity.getState();
            log.debug("Activating from {}: {} ...", previous, activity);
            activity.setState(ActivityState.ACTIVE);
            try {
                activity.onStateChanged(previous, activity.getState(), locale, this);
            } catch (Exception e) {
                throw new ReasonerException(e, "Error while activating %s", activity);
            }

            if (activity instanceof PromptTask) {
                log.debug("Focusing {} ...", activity);
                this.focusedTask = (Task) activity;
            }

            pollActions(locale);
        }
    }

    /**
     * Background activities can only match {@link UtterancePattern}
     * with scope {@link org.lskk.lumen.reasoner.activity.UtterancePattern.Scope#GLOBAL}.
     * You can't modify this! Use {@link #add(Activity)}.
     * FIXME: this shouldn't be "InteractionTask" behavior but a stateful DTO, i.e. TaskExec
     *
     * @return
     */
    public List<Activity> getActivities() {
        return ImmutableList.copyOf(activities);
    }

    public List<Locale> getActiveLocales() {
        return activeLocales;
    }

    /**
     * If the task (usually {@link #getFocusedTask()} has pending actions, poll them:
     * <p>
     * <ol>
     * <li>{@link Task#getLabelsToAssert()}</li>
     * <li>{@link Task#getLiteralsToAssert()}</li>
     * <li>{@link Task#getPendingCommunicateActions()}</li>
     * <li>{@link Task#getPendingPropositions()}</li>
     * </ol>
     *
     * @param locale
     * @see #update(Channel)
     */
    protected void pollActions(Locale locale) {
        activities.forEach(activity -> {
            activity.pollActions(this, locale);

            if (activity instanceof Task) {
                // TODO: replace with SyncLiteralTask/SyncLabelTask/SyncStatementTask
                final Task task = (Task) activity;
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
        });
    }

    // TODO: should parameters replaced by CommunicateAction?
    public void receiveUtterance(Locale locale, String text, FactService factService, TaskRepository taskRepo) {
        lastLocale = locale;
        final CommunicateAction communicateAction = new CommunicateAction(locale, text, null);

        // Check child activities first
        activities.stream().filter(it -> ActivityState.ACTIVE == it.getState()).forEach(activity -> {
            log.trace("Executing receiveUtterance for '{}': {}", activity.getPath(), communicateAction);
            activity.receiveUtterance(communicateAction, this, focusedTask);
        });
        pollActions(locale);

        // consult daemon skills
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
            launchSkill(best.get(), skillRepo, taskRepo);
        } else if (best.isPresent()) {
            log.info("Best intent confidence is < {}, skipped {}/{}: {}", INTENT_MIN_CONFIDENCE,
                    best.get().getSkill().getId(), best.get().getIntent().getId());
        }
    }

    /**
     * Create all {@link Activity}s of a {@link org.lskk.lumen.reasoner.skill.Skill} based
     * on matched {@link UtterancePattern}.
     *
     * @param utterancePattern
     * @param skillRepo
     */
    protected void launchSkill(UtterancePattern utterancePattern, SkillRepository skillRepo, TaskRepository taskRepo) {
        // Sanity check: Make sure you don't already have this skill in the session
        final Optional<Activity> existing = activities.stream().filter(it -> it instanceof Skill && utterancePattern.getSkill().getId().equals(it.getId()))
                .findAny();
        if (existing.isPresent()) {
            throw new ReasonerException(String.format(
                "Invalid attempt to launch already added skill '%s' from %s", existing.get().getPath(), utterancePattern));
        }

        log.info("Launching {}/{} ...", utterancePattern.getSkill().getId(), utterancePattern.getIntent().getId());
        final Skill skill = skillRepo.createAndInitialize(utterancePattern.getSkill().getId());
        // instantiate Skill's child Activities from TaskRef-s
        for (final TaskRef taskRef : skill.getActivityRefs()) {
            final Task task;
            if ("prompt".equals(taskRef.getScheme())) {
                task = taskRepo.createPrompt(taskRef.getId());
            } else if ("affirmation".equals(taskRef.getScheme())) {
                task = taskRepo.createAffirmation(taskRef.getId());
            } else {
                throw new ReasonerException(String.format("Cannot launch skill '%s', unsupported task reference '%s'",
                        utterancePattern.getSkill().getId(), taskRef.getHref()));
            }
            skill.add(task);
        }
        add(skill);
        activate(skill, Locale.forLanguageTag(utterancePattern.getInLanguage()));
    }

    /**
     * Used by {@link #update(Channel)} to express all pending propositions.
     *
     * @param channel
     * @param avatarId
     */
    protected void expressAll(Channel<?> channel, String avatarId) {
        visitFirst(activity -> {
            log.trace("expressAll() visiting activity '{}'", activity.getPath());
            if (activity instanceof Task) {
                final Task task = (Task) activity;
                while (true) {
                    final CommunicateAction pendingCommunicateAction = task.getPendingCommunicateActions().poll();
                    if (null == pendingCommunicateAction) {
                        break;
                    }
                    channel.express(avatarId, pendingCommunicateAction, null);
                }
            }
            return null;
        });
    }

    /**
     * Visits all enabled descendants and return first non-null value.
     * @param visitor
     * @param <R>
     * @return
     */
    public <R> R visitFirst(Function<Activity, R> visitor) {
        return activities.stream().filter(Activity::getEnabled).map(it -> it.visitFirst(visitor))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * Call this to fire pending scheduled activities.
     *
     * @param channel
     */
    public void update(Channel<?> channel) {
        final String avatarId = null;
        expressAll(channel, avatarId); // pre-activation express

        final Activity nextActivity = pendingActivations.poll();
        if (null != nextActivity) {
            activate(nextActivity, getLastLocale());
        }

        expressAll(channel, avatarId); // post-activation express
    }

    public Locale getLastLocale() {
        return lastLocale;
    }

    public Queue<Activity> getPendingActivations() {
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
        if (activity == this.focusedTask) {
            this.focusedTask = null;
        }
        try {
            activity.onStateChanged(previous, activity.getState(), locale, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (activity instanceof Task) {
            pollActions(locale);
        }
    }

    public void schedule(Activity activity) {
        getPendingActivations().add(activity);
    }

    public <T extends Activity> T get(String path) {
        final String firstId = StringUtils.substringBefore(path, ".");
        @Nullable
        final String rest = StringUtils.substringAfter(path, ".");
        final Activity first = Preconditions.checkNotNull(activities.stream().filter(it -> firstId.equals(it.getId())).findAny().orElse(null),
                "Cannot find task '%s' in session %s. %s available activities are: %s",
                firstId, getId(), getActivities().size(), getActivities().stream().map(Activity::getId).collect(Collectors.toList()));
        if (null == rest) {
            return (T) first;
        } else {
            return first.get(rest);
        }
    }

    public Activity add(Activity activity) {
        //activity.setParent(this);
        activities.add(activity);
        return activity;
    }
}

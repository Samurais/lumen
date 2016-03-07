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
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.lskk.lumen.reasoner.skill.ActivityRef;
import org.lskk.lumen.reasoner.skill.Skill;
import org.lskk.lumen.reasoner.skill.SkillRepository;
import org.lskk.lumen.reasoner.ux.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * <p>TO REPEAT: This can NOT extend {@link Activity}, for the simple reason that it will be replaced by
 * Drools's {@link org.kie.internal.runtime.StatefulKnowledgeSession}!</p>
 * <p>
 * <p>Serializable, state is stored. So either use Spring to create this prototype bean using {@link javax.inject.Provider}
 * or pass service beans in methods.</p>
 * <p>
 * Lifecycle:
 * (new) -open-> Running -close-> (gone)
 * Running -poke-> Running
 * Running -suspend-> Suspended
 * Suspended -resume-> Running
 * </p>
 * <p>
 * <p>Sessions can also be merged. For example, one opens a chat, and they also open a Twitter. Later on Lumen discovers
 * that these are the same user, so the two sessions will be merged into one, enabling seamless interaction.</p>
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

    @Autowired(required = false)
    @Scriptable
    private Map<String, Object> scriptables;
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

    public void open(Channel<?> channel, String avatarId) {
        Preconditions.checkState(!activeLocales.isEmpty(), "Requires at least one active locale");
        lastLocale = activeLocales.get(0);
        log.info("Session {} opened with lastLocale {} and {} active locales: {}",
                lastLocale.toLanguageTag(), activeLocales.stream().map(Locale::toLanguageTag).collect(Collectors.toList()));
        update(channel, avatarId);
    }

    @PreDestroy
    public void close() {
    }

    /**
     * @return
     * @see Scriptable
     * @see Script
     */
    public Map<String, Object> getScriptables() {
        return scriptables;
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
                Preconditions.checkState(this.focusedTask != activity,
                        "Sanity check failed: Trying to activate an already focused task '%s'", activity.getPath());
                log.debug("Defocusing {} for {} ...", this.focusedTask, activity);
                this.focusedTask = null;
            }
        }

        if (ActivityState.PENDING == activity.getState()) {
            final ActivityState previous = activity.getState();
            log.debug("Activating from {}: {} ...", previous, activity);
            activity.setState(ActivityState.ACTIVE);
            try {
                changedState(activity, previous, activity.getState(), locale, this);
            } catch (Exception e) {
                throw new ReasonerException(e, "Error while activating %s", activity);
            }

            if (activity instanceof PromptTask) {
                log.debug("Focusing {} ...", activity);
                this.focusedTask = (Task) activity;
            }

            pollActions(locale);
        } else {
            log.warn("Not activating {} activity '{}'", activity.getState(), activity.getPath());
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
     * @see #update(Channel, String)
     */
    protected void pollActions(Locale locale) {
        // recursively pollActions
        activities.forEach(parents -> parents.pollActions(this, locale));

        // grab the result of those
        visitFirst(activity -> {
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

            return null;
        });
    }

    // TODO: should parameters replaced by CommunicateAction?
    // TODO: avatarId
    public void receiveUtterance(@Nullable Optional<Locale> locale, String text, FactService factService, TaskRepository taskRepo, ScriptRepository scriptRepo) {
        printAllStates();

        if (locale.isPresent()) {
            lastLocale = locale.get();
        }
        final CommunicateAction communicateAction = new CommunicateAction(locale.orElse(null), text, null);

        // Check ACTIVE child activities first
        // Skill is different, it's consulted even if PENDING, as long as it's enabled
        final List<Activity> activeActivities = activities.stream().filter(Activity::getEnabled)
                .filter(it -> ActivityState.ACTIVE == it.getState() || (it instanceof Skill && ActivityState.PENDING == it.getState()))
                .collect(Collectors.toList());
        log.debug("Executing receiveUtterance for {} active activities or Skills {}: {}", activeActivities.size(),
                activeActivities.stream().map(Activity::getPath).collect(Collectors.toList()), communicateAction);
        activeActivities.forEach(activity -> {
            log.trace("Executing receiveUtterance for {} {} '{}': {}",
                    activity.getState(), activity.getClass().getSimpleName(), activity.getPath(), communicateAction);
            if (ActivityState.PENDING == activity.getState()) {
                activate(activity, locale.orElse(lastLocale));
            }
            activity.receiveUtterance(communicateAction, this, focusedTask);
        });
        pollActions(locale.orElse(lastLocale));

        // consult daemon skills, only if enabled and only if not yet added to this session
        final Set<String> addedSkills = activities.stream().filter(it -> it instanceof Skill).map(Activity::getId).collect(Collectors.toSet());
        final List<Skill> enabledSkills = skillRepo.getSkills().values().stream()
                .filter(it -> it.getEnabled() && !addedSkills.contains(it.getId())).collect(Collectors.toList());
        final List<UtterancePattern> totalMatches = enabledSkills.stream().flatMap(skill -> {
            skill.resolveIntents(taskRepo);
            final List<UtterancePattern> skillMatches = skill.getIntents().stream().flatMap(intent -> {
                final List<UtterancePattern> matches = intent.matchUtterance(locale, text, UtterancePattern.Scope.GLOBAL);
                matches.forEach(match -> {
                    match.setIntent(intent);
                    match.setSkill(skill);
                });
                if (!matches.isEmpty()) {
                    log.debug("Repository Skill '{}' intent '{}' returned {} matches for utterance '{}'@{}",
                            skill.getId(), intent.getId(), matches.size(), text, locale.orElse(null));
                }
                return matches.stream();
            }).collect(Collectors.toList());
            log.debug("Repository Skill '{}' with {} intents {} returned {} matches for '{}'@{}",
                    skill.getId(), skill.getIntents().size(), skill.getIntents().stream().map(Activity::getId).toArray(), skillMatches.size(),
                    text, locale.orElse(null));
            return skillMatches.stream();
        }).sorted(new IConfidence.Comparator()).collect(Collectors.toList());
        log.info("Total {} matches for '{}'@{} from {} enabled+unadded repository skills ({}):\n{}",
                totalMatches.size(), text, locale.orElse(null), enabledSkills.size(),
                enabledSkills.stream().map(Skill::getId).collect(Collectors.toList()),
                totalMatches.stream().limit(10)
                        .map(m -> String.format("* %s/%s: %s", m.getSkill().getId(), m.getIntent().getId(), m))
                        .collect(Collectors.joining("\n")));

        final Optional<UtterancePattern> best = totalMatches.stream().findFirst();
        if (best.isPresent() && best.get().getConfidence() >= INTENT_MIN_CONFIDENCE) {
            final Skill skill = launchSkill(best.get(), skillRepo, taskRepo, scriptRepo);
            skill.receiveUtterance(new CommunicateAction(locale.orElse(null), text, null), this, focusedTask);
        } else if (best.isPresent()) {
            log.info("Best intent confidence is < {}, skipped {}/{}: {}", INTENT_MIN_CONFIDENCE,
                    best.get().getSkill().getId(), best.get().getIntent().getId());
        }
    }

    /**
     * TODO: Support avatarId.
     * @param locale
     * @param text
     * @param factService
     * @param taskRepo
     * @param scriptRepo
     * @return
     */
    public AgentResponse receiveUtteranceForResponse(Optional<Locale> locale, String text, FactService factService, TaskRepository taskRepo, ScriptRepository scriptRepo) {
        final String avatarId = "anime1";

        receiveUtterance(locale, text, factService, taskRepo, scriptRepo);
        final List<CommunicateAction> replies = expressAllForResponse(avatarId);// pre-activation express

        final Activity nextActivity = pendingActivations.poll();
        if (null != nextActivity) {
            activate(nextActivity, getLastLocale());
        }

        replies.addAll(expressAllForResponse(avatarId)); // post-activation express

        final AgentResponse agentResponse = new AgentResponse(text);
        agentResponse.setStimuliLanguage(locale.orElse(null));
        agentResponse.getCommunicateActions().addAll(replies);
        return agentResponse;
    }

    /**
     * Create all {@link Activity}s based on matched {@link UtterancePattern} which can be either:
     * {@link org.lskk.lumen.reasoner.skill.Skill} with auto-start intent, or
     * {@link org.lskk.lumen.reasoner.skill.Skill} with intent from {@link UtterancePattern#getIntent()}.
     *
     * @param utterancePattern
     * @param skillRepo
     * @param scriptRepo
     */
    protected Skill launchSkill(UtterancePattern utterancePattern, SkillRepository skillRepo, TaskRepository taskRepo, ScriptRepository scriptRepo) {
        // Sanity check: Make sure you don't already have this skill in the session
        final Optional<Activity> existing = activities.stream().filter(it -> it instanceof Skill && utterancePattern.getSkill().getId().equals(it.getId()))
                .findAny();
        if (existing.isPresent()) {
            throw new ReasonerException(String.format(
                    "Invalid attempt to launch already added skill '%s' from %s", existing.get().getPath(), utterancePattern));
        }

        log.info("Launching {}/{} ...", utterancePattern.getSkill().getId(), utterancePattern.getIntent().getId());
        final Skill skill = skillRepo.createOnly(utterancePattern.getSkill().getId());
        // instantiate Skill's child Activities from TaskRef-s
        for (final ActivityRef activityRef : skill.getActivityRefs()) {
            final Activity child;
            if ("prompt".equals(activityRef.getScheme())) {
                child = taskRepo.createPrompt(activityRef.getId());
            } else if ("affirmation".equals(activityRef.getScheme())) {
                child = taskRepo.createAffirmation(activityRef.getId());
            } else if ("script".equals(activityRef.getScheme())) {
                child = scriptRepo.createScript(activityRef.getId());
            } else {
                throw new ReasonerException(String.format("Cannot launch skill '%s', unsupported activity reference '%s'",
                        utterancePattern.getSkill().getId(), activityRef.getHref()));
            }
            skill.add(child);
        }
        add(skill);
        skill.initialize();
        final Locale locale = Locale.forLanguageTag(utterancePattern.getInLanguage());
        activate(skill, locale);
        if (null != utterancePattern.getIntent()) {
            final Activity realIntent = skill.get(utterancePattern.getIntent().getId());
            // when Skill is activated, this intent may already be active, so do not blindly re-activate
            if (ActivityState.ACTIVE != realIntent.getState()) {
                activate(realIntent, locale);
            }
        } else {
            skill.autoStart(locale, this);
        }
        return skill;
    }

    /**
     * Used by {@link #update(Channel, String)} to express all pending propositions.
     *
     * @param channel
     * @param avatarId
     * @see #expressAllForResponse(String)
     */
    protected void expressAll(Channel<?> channel, String avatarId) {
        visitFirst(activity -> {
            log.trace("Session {} expressAll() visiting activity '{}'", id, activity.getPath());
            while (true) {
                final CommunicateAction pendingCommunicateAction = activity.getPendingCommunicateActions().poll();
                if (null == pendingCommunicateAction) {
                    break;
                }
                channel.express(avatarId, pendingCommunicateAction, null);
            }
            return null;
        });
    }

    /**
     *
     * @return
     * @see #expressAll(Channel, String)
     * @see #receiveUtteranceForResponse(Optional, String, FactService, TaskRepository, ScriptRepository)
     * @param avatarId
     */
    protected List<CommunicateAction> expressAllForResponse(String avatarId) {
        final ArrayList<CommunicateAction> communicateActions = new ArrayList<>();
        visitFirst(activity -> {
            log.trace("Session {} expressAll() visiting activity '{}'", id, activity.getPath());
            while (true) {
                final CommunicateAction pendingCommunicateAction = activity.getPendingCommunicateActions().poll();
                if (null == pendingCommunicateAction) {
                    break;
                }
                pendingCommunicateAction.setAvatarId(avatarId);
                communicateActions.add(pendingCommunicateAction);
            }
            return null;
        });
        return communicateActions;
    }

    /**
     * Visits all enabled descendants and return first non-null value.
     *
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
     * @param avatarId
     */
    public void update(Channel<?> channel, String avatarId) {
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
            changedState(activity, previous, activity.getState(), locale, this);
        } catch (Exception e) {
            throw new ReasonerException(e, "Cannot complete activity '%s'", activity.getPath());
        }
        if (activity instanceof Task) {
            pollActions(locale);
        }
    }

    /**
     * Visit all of specified {@link Activity}'s descendants and sets their state back to {@link ActivityState#PENDING}.
     */
    public void reset(Activity parent, Locale locale) {
        final ActivityState previousForParent = parent.getState();
        parent.visitFirst(act -> {
            final ActivityState previous = act.getState();
            if (ActivityState.COMPLETED == previous) {
                act.setState(ActivityState.PENDING);
                try {
                    changedState(act, previous, act.getState(), locale, this);
                } catch (Exception e) {
                    throw new ReasonerException(e, "Cannot reset state of '%s' for descendant '%s'",
                            parent.getPath(), act.getPath());
                }
            }
            return null;
        });
//        if (ActivityState.COMPLETED == previousForParent) {
//            parent.setState(ActivityState.ACTIVE);
//            try {
//                changedState(parent, previousForParent, parent.getState(), locale, this);
//            } catch (Exception e) {
//                throw new ReasonerException(e, "Cannot reset state of '%s' for parent", parent.getPath());
//            }
//        }
    }

    /**
     * Calls {@link Activity#onStateChanged(ActivityState, ActivityState, Locale, InteractionSession)}
     * of an {@link Activity}, and bubbling up to all of that activity's parents.
     *
     * @param target
     * @param previous
     * @param current
     * @param locale   Specific {@link Locale} that was active during the state change, it's always one of {@link InteractionSession#getActiveLocales()}.
     * @param session
     * @throws Exception
     */
    protected void changedState(final Activity target, ActivityState previous, ActivityState current, Locale locale, InteractionSession session) throws Exception {
        Activity curActivity = target;
        do {
            if (target == curActivity) {
                curActivity.onStateChanged(previous, target.getState(), locale, this);
            } else {
                curActivity.onChildStateChanged(target, previous, target.getState(), locale, this);
            }
            // bubble-up
            curActivity = curActivity.getParent();
        } while (null != curActivity);

        printAllStates();
    }

    /**
     * Debugging tool to print all activity's states.
     */
    public void printAllStates() {
        final StringBuffer sb = new StringBuffer();
        visitFirst(act -> {
            sb.append(String.format("%s %s\n", act.getPath(), act.getState()));
            return null;
        });
        log.info("Session {} focus={}, states:\n{}", getId(), focusedTask != null ? focusedTask.getPath() : null, sb);
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

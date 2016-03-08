package org.lskk.lumen.reasoner.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.activity.*;
import org.lskk.lumen.reasoner.intent.Slot;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 03/03/2016.
 */
public class Skill extends Activity {

    private List<ActivityRef> activityRefs = new ArrayList<>();
    /**
     * Initially {@code null} until {@link #resolveIntents(TaskRepository, ScriptRepository)} is called by {@link InteractionSession#receiveUtterance(Optional, String, String, FactService, TaskRepository, ScriptRepository)}.
     */
    private List<Activity> intents;
    private List<Connection> connections = new ArrayList<>();

    public Skill() {
        super();
    }

    public Skill(String id) {
        super(id);
    }

    @JsonProperty("activities")
    public List<ActivityRef> getActivityRefs() {
        return activityRefs;
    }

    public List<Activity> getIntents() {
        return intents;
    }

    /**
     * Resolve {@link ActivityRef}s into {@link ActivityRef#setInstance(Activity)}.
     * For {@link ActivityRef#getIntentCapturing()} of {@code true}, will be added to {@link #getIntents()}.
     * Only called once, further calls have no effect.
     * @param taskRepo
     * @param scriptRepo
     */
    public void resolveIntents(TaskRepository taskRepo, ScriptRepository scriptRepo) {
        if (null == intents) {
            intents = new ArrayList<>();
            activityRefs.stream().forEach(actitivyRef -> {
                log.info("Skill '{}' resolving {} {} task '{}'", getPath(),
                        Boolean.TRUE == actitivyRef.getIntentCapturing() ? "intent-capturing" : "regular", actitivyRef.getScheme(), actitivyRef.getId());
                final Activity instance;
                switch (actitivyRef.getScheme()) {
                    case "prompt":
                        instance = taskRepo.createPrompt(actitivyRef.getId());
                        break;
                    case "affirmation":
                        instance = taskRepo.createAffirmation(actitivyRef.getId());
                        break;
                    case "script":
                        instance = scriptRepo.createScript(actitivyRef.getId());
                        break;
                    default:
                        throw new ReasonerException(String.format("Cannot resolve skill '%s', unsupported activity reference '%s'",
                                getPath(), actitivyRef.getHref()));
                }
                actitivyRef.setInstance(instance);
                if (Boolean.TRUE == actitivyRef.getIntentCapturing()) {
                    intents.add(instance);
                }
            });
        }
    }

    public List<Connection> getConnections() {
        return connections;
    }

    @Override
    public void onStateChanged(ActivityState previous, ActivityState current, Locale locale, InteractionSession session) throws Exception {
        super.onStateChanged(previous, current, locale, session);
        if (ActivityState.COMPLETED == current) {
            log.info("Resetting completed skill '{}' with {} children: {}",
                    getPath(), getActivities().size(), getActivities().stream().map(Activity::getId).collect(Collectors.toList()));
            session.reset(this, locale);
        }
    }

    /**
     * Called by {@link InteractionSession#launchSkill(UtterancePattern, SkillRepository, TaskRepository, ScriptRepository)}
     * if an activated Skill needs to auto-start a child activity.
     * @param locale
     * @param session
     * @deprecated {@link #pollActions(InteractionSession, Locale)} automatically activates enabled & ready activity.
     */
    @Deprecated
    public void autoStart(Locale locale, InteractionSession session) {
//        // Activate first auto-start and ready child activity
//        final Optional<Activity> child = getActivities().stream()
//                .filter(act -> act.isReady() && act.getAutoStart()).findFirst();
//        if (child.isPresent()) {
//            session.activate(child.get(), locale);
//        }
    }

    @Override
    public void pollActions(InteractionSession session, Locale locale) {
        super.pollActions(session, locale);
        // Pass 1: scan connections and group by source
        final ArrayListMultimap<String, Connection> connectionsBySource = ArrayListMultimap.create();
        connections.forEach(conn -> {
            connectionsBySource.put(conn.getSource(), conn);
        });
        // Pass 2: move packets. we do this in separate phase because one source may be connected to multiple sinks
        // (a deviation from classical flow-based programming)
        connectionsBySource.asMap().forEach((source, conns) -> {
            final Connection firstConn = conns.iterator().next();
            final Activity sourceActivity = get(firstConn.getSourceActivity());
            final Queue<Object> sourceQueue = sourceActivity.getOutSlot(firstConn.getSourceSlot()).getOutQueue();
            if (!sourceQueue.isEmpty()) {
                final List<Object> flows = new ArrayList<>();
                while (!sourceQueue.isEmpty()) {
                    final Object obj = sourceQueue.poll();
                    flows.add(obj);
                    conns.forEach(sink -> {
                        final Activity sinkActivity = get(sink.getSinkActivity());
                        final Slot sinkSlot = sinkActivity.getInSlot(sink.getSinkSlot());
                        sinkSlot.add(obj);
                    });
                }
                conns.forEach(sink -> {
                    final Activity sinkActivity = get(sink.getSinkActivity());
                    log.debug("Moved {}({}) {} -[{}]-> {} {}({}): {}",
                            sourceActivity.getPath(), sourceActivity.getClass().getSimpleName(), firstConn.getSourceSlot(),
                            flows.size(), sink.getSinkSlot(), sinkActivity.getPath(), sinkActivity.getClass().getSimpleName(), flows);
                });
            }
        });

        // if we're ACTIVE, then activate PENDING and ready child activities
        if (ActivityState.ACTIVE == getState()) {
            getActivities().stream().filter(act -> ActivityState.PENDING == act.getState() && act.isReady())
                    .forEach(act -> {
                        log.debug("{} '{}' requesting activation of pending and ready activity '{}'", getClass().getSimpleName(), getPath(), act.getId());
                        session.activate(act, locale);
                    });
        }
    }

    protected enum SyntaxKind {
        START,
        STOP,
        RECEIVE,
        SEND,
        ACTION,
        ARROW
    }

    protected static class Syntax implements Serializable {
        protected SyntaxKind kind;
        protected String name;
        protected String note;

        public Syntax(SyntaxKind kind, String name) {
            this.kind = kind;
            this.name = name;
            this.note = null;
        }

        public Syntax(SyntaxKind kind, String name, String note) {
            this.kind = kind;
            this.name = name;
            this.note = note;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).omitNullValues()
                    .add("kind", kind)
                    .add("name", name)
                    .toString();
        }
    }

    public String renderUml() {
        // convert to syntax/railroad diagram
        final List<Syntax> syntaxGraph = new ArrayList<>();
        getActivityRefs().stream().filter(it -> Boolean.TRUE.equals(it.getIntentCapturing()))
                .forEach(it -> {
                    syntaxGraph.add(new Syntax(SyntaxKind.START, null));
                    syntaxGraph.add(new Syntax(SyntaxKind.RECEIVE, it.getId()));
                    syntaxGraph.add(new Syntax(SyntaxKind.STOP, null));
                });

        getConnections().forEach(conn -> {
            Syntax source = syntaxGraph.stream().filter(it -> conn.getSourceActivity().equals(it.name)).findAny().orElse(null);
            Preconditions.checkNotNull(source, "Cannot find action '" + conn.getSourceActivity() + "' in current syntax graph: " + syntaxGraph);
            Syntax sink = syntaxGraph.stream().filter(it -> conn.getSinkActivity().equals(it.name)).findAny().orElse(null);
            final Syntax arrow;
            if (sink == null) {
                arrow = new Syntax(SyntaxKind.ARROW, conn.getSourceSlot());
                sink = new Syntax(conn.getSinkActivity().startsWith("affirm") ? SyntaxKind.SEND : SyntaxKind.ACTION, conn.getSinkActivity());
                syntaxGraph.add(syntaxGraph.indexOf(source) + 1, arrow);
                syntaxGraph.add(syntaxGraph.indexOf(source) + 2, sink);
            } else {
                arrow = syntaxGraph.get(syntaxGraph.indexOf(sink) - 1);
                arrow.name += ", " + conn.getSourceSlot();
            }
        });


        String uml = "";
        uml += "@startuml\n";
        uml += "\n";
        uml += "header\n";
        uml += "<b>" + getId() + "</b>: " + getDescription() + "\n";
        uml += "endheader\n";
        uml += "\n";
        for (Syntax syntax : syntaxGraph) {
            switch (syntax.kind) {
                case START:
                    uml += "start\n";
                    break;
                case STOP:
                    uml += "stop\n";
                    break;
                case RECEIVE:
                    uml += ":" + syntax.name + "<\n";
                    if (syntax.note != null) {
                        uml += "note left\n";
                        uml += syntax.note + "\n";
                        uml += "end note\n";
                    }
                    break;
                case SEND:
                    uml += ":" + syntax.name + ">\n";
                    if (syntax.note != null) {
                        uml += "note left\n";
                        uml += syntax.note + "\n";
                        uml += "end note\n";
                    }
                    break;
                case ACTION:
                    uml += ":" + syntax.name + ";\n";
                    if (syntax.note != null) {
                        uml += "note left\n";
                        uml += syntax.note + "\n";
                        uml += "end note\n";
                    }
                    break;
                case ARROW:
                    uml += "-> " + syntax.name + ";\n";
                    break;
            }
        }
        uml += "@enduml\n";
        return uml;
    }
}

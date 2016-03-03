package org.lskk.lumen.reasoner.skill;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.interaction.InteractionTask;
import org.lskk.lumen.reasoner.interaction.InteractionTaskRepository;
import org.lskk.lumen.reasoner.interaction.PromptTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by ceefour on 03/03/2016.
 */
public class Skill implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(Skill.class);

    private String id;
    private String name;
    private String description;
    private List<TaskRef> tasks = new ArrayList<>();
    private List<InteractionTask> intents = new ArrayList<>();
    private List<Connection> connections = new ArrayList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TaskRef> getTasks() {
        return tasks;
    }

    public List<InteractionTask> getIntents() {
        return intents;
    }

    /**
     * Resolve {@link TaskRef}s with {@link TaskRef#getIntentCapturing()} of {@code true}, to
     * {@link org.lskk.lumen.reasoner.interaction.InteractionTask}s.
     * @param taskRepo
     */
    public void resolveIntents(InteractionTaskRepository taskRepo) {
        intents.clear();
        tasks.stream().filter(it -> Boolean.TRUE.equals(it.getIntentCapturing())).forEach(taskRef -> {
            final String taskId = StringUtils.substringAfter(taskRef.getHref(), ":");
            log.info("Skill '{}' resolving intent-capturing PromptTask '{}'", getId(), taskId);
            final PromptTask promptTask = taskRepo.createPrompt(taskId);
            intents.add(promptTask);
        });
    }

    public List<Connection> getConnections() {
        return connections;
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
        getTasks().stream().filter(it -> Boolean.TRUE.equals(it.getIntentCapturing()))
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

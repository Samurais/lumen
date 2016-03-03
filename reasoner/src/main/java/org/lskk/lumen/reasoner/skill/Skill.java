package org.lskk.lumen.reasoner.skill;

import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.reasoner.interaction.InteractionTask;
import org.lskk.lumen.reasoner.interaction.InteractionTaskRepository;
import org.lskk.lumen.reasoner.interaction.PromptTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
}

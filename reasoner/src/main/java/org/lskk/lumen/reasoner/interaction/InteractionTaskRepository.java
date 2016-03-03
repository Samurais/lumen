package org.lskk.lumen.reasoner.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.reasoner.ReasonerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides ready-made {@link PromptTask}s from {@code classpath:/org/lskk/lumen/reasoner/interaction/*.PromptTask.json}
 * Created by ceefour on 25/02/2016.
 */
@Repository
public class InteractionTaskRepository {

    private static final Logger log = LoggerFactory.getLogger(InteractionTaskRepository.class);
    private Map<String, InteractionTask> taskProtos = new LinkedHashMap<>();

    @Inject
    private ObjectMapper mapper;

    @PostConstruct
    public void init() throws IOException {
        // PromptTasks
        final Resource[] promptResources = new PathMatchingResourcePatternResolver(InteractionTaskRepository.class.getClassLoader())
                .getResources("classpath*:org/lskk/lumen/reasoner/interaction/*.PromptTask.json");
        for (final Resource res : promptResources) {
            final String id = StringUtils.substringBefore(res.getFilename(), ".");
            log.debug("Loading '{}' from {} ...", id, res);
            final PromptTask promptTaskProto = mapper.readValue(res.getURL(), PromptTask.class);
            promptTaskProto.setId(id);
            taskProtos.put(id, promptTaskProto);
        }
        final List<InteractionTask> promptTasks = taskProtos.values().stream().filter(it -> it instanceof PromptTask).collect(Collectors.toList());
        log.info("Loaded {} PromptTasks: {}", promptTasks.size(), promptTasks.stream().map(InteractionTask::getId).toArray());

        // AffirmationTasks
        final Resource[] affirmationResources = new PathMatchingResourcePatternResolver(InteractionTaskRepository.class.getClassLoader())
                .getResources("classpath*:org/lskk/lumen/reasoner/interaction/*.AffirmationTask.json");
        for (final Resource res : affirmationResources) {
            final String id = StringUtils.substringBefore(res.getFilename(), ".");
            log.debug("Loading '{}' from {} ...", id, res);
            final AffirmationTask proto = mapper.readValue(res.getURL(), AffirmationTask.class);
            proto.setId(id);
            taskProtos.put(id, proto);
        }
        final List<InteractionTask> affirmationTasks = taskProtos.values().stream().filter(it -> it instanceof AffirmationTask).collect(Collectors.toList());
        log.info("Loaded {} AffirmationTasks: {}",
                affirmationTasks.size(), affirmationTasks.stream().map(InteractionTask::getId).toArray());
    }

    /**
     * Create a new instance, this will load the task descriptor from {@code classpath*:org/lskk/lumen/reasoner/interaction/ID.PromptTask.json}
     * so it is JRebel-friendly.
     * @param id
     * @return
     */
    public PromptTask createPrompt(String id) {
        final URL res = InteractionTaskRepository.class.getResource("/org/lskk/lumen/reasoner/interaction/" + id + ".PromptTask.json");
        try {
            final PromptTask task = mapper.readValue(res, PromptTask.class);
            task.setId(id);
            return task;
        } catch (IOException e) {
            final Stream<InteractionTask> promptTasks = taskProtos.values().stream().filter(it -> it instanceof PromptTask);
            throw new ReasonerException(e, "Cannot create PromptTask '%s'. %s available PromptTasks: %s",
                    id, promptTasks.count(), promptTasks.map(InteractionTask::getId).toArray());
        }
    }

    /**
     * Create a new instance, this will load the task descriptor from {@code classpath*:org/lskk/lumen/reasoner/interaction/ID.AffirmationTask.json}
     * so it is JRebel-friendly.
     * @param id
     * @return
     */
    public AffirmationTask createAffirmation(String id) {
        final String url = "/org/lskk/lumen/reasoner/interaction/" + id + ".AffirmationTask.json";
        final URL res = Preconditions.checkNotNull(InteractionTaskRepository.class.getResource(url),
                "Affirmation '%s' not found in classpath: %s", id, url);
        try {
            final AffirmationTask task = mapper.readValue(res, AffirmationTask.class);
            task.setId(id);
            return task;
        } catch (IOException e) {
            final Stream<InteractionTask> affirmationTasks = taskProtos.values().stream().filter(it -> it instanceof AffirmationTask);
            throw new ReasonerException(e, "Cannot create AffirmationTask '%s'. %s available AffirmationTasks: %s",
                    id, affirmationTasks.count(), affirmationTasks.map(InteractionTask::getId).toArray());
        }
    }

    /**
     * These are only prototypes that are immutable, do not use them as is!
     * @return
     */
    public Map<String, InteractionTask> getTaskProtos() {
        return taskProtos;
    }
}

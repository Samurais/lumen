package org.lskk.lumen.reasoner.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides ready-made {@link PromptTask}s from {@code classpath:/org/lskk/lumen/reasoner/interaction/*.PromptTask.json}
 * Created by ceefour on 25/02/2016.
 */
@Repository
public class PromptTaskRepository {

    private static final Logger log = LoggerFactory.getLogger(PromptTaskRepository.class);
    private Map<String, PromptTask> promptTasks = new LinkedHashMap<>();

    @Inject
    private ObjectMapper mapper;

    @PostConstruct
    public void init() throws IOException {
        final Resource[] resources = new PathMatchingResourcePatternResolver(PromptTaskRepository.class.getClassLoader())
                .getResources("classpath*:org/lskk/lumen/reasoner/interaction/*.PromptTask.json");
        for (final Resource res : resources) {
            final String id = StringUtils.substringBefore(res.getFilename(), ".");
            log.debug("Loading '{}' from {} ...", id, res);
            final PromptTask promptTask = mapper.readValue(res.getURL(), PromptTask.class);
            promptTasks.put(id, promptTask);
        }
        log.info("Loaded {} PromptTasks: {}", promptTasks.size(), promptTasks.keySet());
    }

    public PromptTask get(String id) {
        return Preconditions.checkNotNull(promptTasks.get(id),
                "Cannot find PromptTask '%s'. %s available PromptTasks: %s",
                id, promptTasks.size(), promptTasks.keySet());
    }

    public Map<String, PromptTask> getPromptTasks() {
        return promptTasks;
    }
}

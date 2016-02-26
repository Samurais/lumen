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
import java.util.Map;

/**
 * Provides ready-made {@link PromptTask}s from {@code classpath:/org/lskk/lumen/reasoner/interaction/*.PromptTask.json}
 * Created by ceefour on 25/02/2016.
 */
@Repository
public class PromptTaskRepository {

    private static final Logger log = LoggerFactory.getLogger(PromptTaskRepository.class);
    private Map<String, PromptTask> promptTaskProtos = new LinkedHashMap<>();

    @Inject
    private ObjectMapper mapper;

    @PostConstruct
    public void init() throws IOException {
        final Resource[] resources = new PathMatchingResourcePatternResolver(PromptTaskRepository.class.getClassLoader())
                .getResources("classpath*:org/lskk/lumen/reasoner/interaction/*.PromptTask.json");
        for (final Resource res : resources) {
            final String id = StringUtils.substringBefore(res.getFilename(), ".");
            log.debug("Loading '{}' from {} ...", id, res);
            final PromptTask promptTaskProto = mapper.readValue(res.getURL(), PromptTask.class);
            promptTaskProto.setId(id);
            promptTaskProtos.put(id, promptTaskProto);
        }
        log.info("Loaded {} PromptTasks: {}", promptTaskProtos.size(), promptTaskProtos.keySet());
    }

    /**
     * Create a new instance, this will load the task descriptor from {@code classpath*:org/lskk/lumen/reasoner/interaction/ID.PromptTask.json}
     * so it is JRebel-friendly.
     * @param id
     * @return
     */
    public PromptTask create(String id) {
        final URL res = PromptTaskRepository.class.getResource("/org/lskk/lumen/reasoner/interaction/" + id + ".PromptTask.json");
        try {
            final PromptTask promptTask = mapper.readValue(res, PromptTask.class);
            promptTask.setId(id);
            return promptTask;
        } catch (IOException e) {
            throw new ReasonerException(e, "Cannot create PromptTask '%s'. %s available PromptTasks: %s",
                    id, promptTaskProtos.size(), promptTaskProtos.keySet());
        }
    }

    /**
     * These are only prototypes that are immutable, do not use them as is!
     * @return
     */
    public Map<String, PromptTask> getPromptTaskProtos() {
        return promptTaskProtos;
    }
}

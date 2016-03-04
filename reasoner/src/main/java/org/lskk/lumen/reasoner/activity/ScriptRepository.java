package org.lskk.lumen.reasoner.activity;

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
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Created by ceefour on 03/03/2016.
 */
@Repository
public class ScriptRepository {

    private static final Logger log = LoggerFactory.getLogger(ScriptRepository.class);

    private Map<String, Script> scripts = new TreeMap<>();

    @Inject
    private ObjectMapper mapper;

    public Map<String, Script> getScripts() {
        return scripts;
    }

    @PostConstruct
    public void init() throws IOException {
        final Resource[] scriptResources = new PathMatchingResourcePatternResolver(ScriptRepository.class.getClassLoader())
                .getResources("classpath*:org/lskk/lumen/reasoner/activity/*.Script.json");
        for (final Resource res : scriptResources) {
            final String id = StringUtils.substringBefore(res.getFilename(), ".");
            log.debug("Loading '{}' from {} ...", id, res);
            final Script script = mapper.readValue(res.getURL(), Script.class);
            script.setId(id);
            script.initialize();
            scripts.put(id, script);
//            log.debug("Script '{}' contains {} tasks: {}", script.getId(),
//                    script.getTasks().size(), script.getTasks().stream().map(TaskRef::getHref).toArray());
        }
        log.info("Loaded {} Scripts: {}", scripts.size(), scripts.keySet());
    }

    public Script get(String id) {
        return Preconditions.checkNotNull(scripts.get(id),
                "Cannot get script '%s'. %s available scripts: %s", id, scripts.size(), scripts.keySet());
    }

    /**
     * Create a new instance, this will on-demand load the script descriptor from
     * {@code classpath*:org/lskk/lumen/reasoner/activity/ID.Script.json}
     * so it is JRebel-friendly. While script execution is always on-demand load by {@link Script} itself.
     * @param id
     * @return
     */
    public Script createScript(String id) {
        final String url = "/org/lskk/lumen/reasoner/activity/" + id + ".Script.json";
        final URL res = Preconditions.checkNotNull(TaskRepository.class.getResource(url),
                "Affirmation '%s' not found in classpath: %s", id, url);
        try {
            final Script script = mapper.readValue(res, Script.class);
            script.setId(id);
            return script;
        } catch (IOException e) {
            throw new ReasonerException(e, "Cannot create Script '%s'. %s available Scripts: %s",
                    id, scripts.size(), scripts.keySet());
        }
    }

}

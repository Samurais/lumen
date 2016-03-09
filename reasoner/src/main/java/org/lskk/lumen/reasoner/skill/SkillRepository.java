package org.lskk.lumen.reasoner.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.activity.ScriptRepository;
import org.lskk.lumen.reasoner.activity.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 03/03/2016.
 */
@Repository
public class SkillRepository {

    private static final Logger log = LoggerFactory.getLogger(SkillRepository.class);

    private Map<String, Skill> skills = new TreeMap<>();

    @Inject
    private ObjectMapper mapper;
    @Autowired(required = false)
    private TaskRepository taskRepo;
    @Autowired(required = false)
    private ScriptRepository scriptRepo;

    public Map<String, Skill> getSkills() {
        return skills;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        try {
            final Resource[] skillResources = new PathMatchingResourcePatternResolver(SkillRepository.class.getClassLoader())
                    .getResources("classpath*:org/lskk/lumen/reasoner/skill/*.Skill.json");
            for (final Resource res : skillResources) {
                final String id = StringUtils.substringBefore(res.getFilename(), ".");
                final Skill skill = createOnly(id, res.getURL());
                skills.put(id, skill);
            }
            log.info("Loaded {} Skills: {}", skills.size(), skills.keySet());

            skills.values().forEach(Skill::initialize);

            if (null != taskRepo && null != scriptRepo) {
                skills.forEach((id, skill) -> skill.resolveIntents(taskRepo, scriptRepo));
            } else {
                log.warn("TaskRepository or ScriptRepository beans not available, will not resolve {} skills: {}",
                        skills.size(), skills.keySet());
            }
        } catch (Exception e) {
            throw new ReasonerException(e, "Error scanning Skills");
        }
    }

    public Skill get(String id) {
        return Preconditions.checkNotNull(skills.get(id),
                "Cannot get skill '%s'. %s available skills: %s", id, skills.size(), skills.keySet());
    }

    public Skill createOnly(String id) {
        final String path = "/org/lskk/lumen/reasoner/skill/" + id + ".Skill.json";
        final URL url = Preconditions.checkNotNull(SkillRepository.class.getResource(path),
                "Cannot find Skill '%s' descriptor in classpath: %s", id, path);
        return createOnly(id, url);
    }

    protected Skill createOnly(String id, URL url) {
        log.debug("Loading '{}' from {} ...", id, url);
        try {
            final Skill skill = mapper.readValue(url, Skill.class);
            skill.setId(id);
            log.debug("Skill '{}' contains {} tasks: {}", skill.getId(),
                    skill.getActivityRefs().size(), skill.getActivityRefs().stream().map(ActivityRef::getHref)
                            .collect(Collectors.toList()));
            return skill;
        } catch (Exception e) {
            throw new ReasonerException(e, "Error loading skill '%s' from '%s'", id, url);
        }
    }

    public void resolveIntents(TaskRepository taskRepo) {
        skills.forEach((k, v) -> v.resolveIntents(taskRepo, scriptRepo));
    }
}

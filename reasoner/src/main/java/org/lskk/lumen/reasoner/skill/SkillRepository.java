package org.lskk.lumen.reasoner.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.reasoner.interaction.InteractionTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by ceefour on 03/03/2016.
 */
@Repository
public class SkillRepository {

    private static final Logger log = LoggerFactory.getLogger(SkillRepository.class);

    private Map<String, Skill> skills = new TreeMap<>();

    @Inject
    private ObjectMapper mapper;

    public Map<String, Skill> getSkills() {
        return skills;
    }

    @PostConstruct
    public void init() throws IOException {
        // PromptTasks
        final Resource[] promptResources = new PathMatchingResourcePatternResolver(SkillRepository.class.getClassLoader())
                .getResources("classpath*:org/lskk/lumen/reasoner/skill/*.Skill.json");
        for (final Resource res : promptResources) {
            final String id = StringUtils.substringBefore(res.getFilename(), ".");
            log.debug("Loading '{}' from {} ...", id, res);
            final Skill skill = mapper.readValue(res.getURL(), Skill.class);
            skill.setId(id);
            skills.put(id, skill);
            log.debug("Skill '{}' contains {} tasks: {}", skill.getId(),
                    skill.getTasks().size(), skill.getTasks().stream().map(TaskRef::getHref).toArray());
        }
        log.info("Loaded {} Skills: {}", skills.size(), skills.keySet());
    }

    public Skill get(String id) {
        return Preconditions.checkNotNull(skills.get(id),
                "Cannot get skill '%s'. %s available skills: %s", id, skills.size(), skills.keySet());
    }

    public void resolveIntents(InteractionTaskRepository taskRepo) {
        skills.forEach((k, v) -> v.resolveIntents(taskRepo));
    }
}

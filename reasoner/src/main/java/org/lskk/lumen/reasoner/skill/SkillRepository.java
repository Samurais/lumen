package org.lskk.lumen.reasoner.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 03/03/2016.
 */
@Repository
public class SkillRepository {

    private static final Logger log = LoggerFactory.getLogger(SkillRepository.class);

    private List<Skill> skills = new ArrayList<>();

    public List<Skill> getSkills() {
        return skills;
    }
}

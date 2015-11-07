package org.lskk.lumen.reasoner.story;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.socmed.AbstractCrudRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 07/11/2015.
 */
@Repository
public class StoryRepository extends AbstractCrudRepository<Story,String> {

    @Inject
    private ObjectMapper mapper;

    public List<Story> stories = new ArrayList<>();

    public List<Story> getStories() {
        return stories;
    }

    @PostConstruct
    public void init() throws IOException {
        stories = mapper.readValue(StoryRepository.class.getResource("/org/lskk/lumen/reasoner/stories.json"),
                new TypeReference<List<Story>>() {});
    }

    @Override
    public Story findOne(String id) {
        return stories.stream().filter(it -> id.equals(it.getId())).findAny().get();
    }
}

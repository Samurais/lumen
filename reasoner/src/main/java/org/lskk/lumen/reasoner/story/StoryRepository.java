package org.lskk.lumen.reasoner.story;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.socmed.AbstractCrudRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
    public <S extends Story> S save(S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends Story> Iterable<S> save(Iterable<S> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Story findOne(String id) {
        return stories.stream().filter(it -> id.equals(it.getId())).findAny().get();
    }

    @Override
    public boolean exists(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Story> findAll() {
        return stories;
    }

    @Override
    public Iterable<Story> findAll(Iterable<String> strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long count() {
        return stories.size();
    }

    @Override
    public void delete(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Story entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Iterable<? extends Story> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException();
    }
}

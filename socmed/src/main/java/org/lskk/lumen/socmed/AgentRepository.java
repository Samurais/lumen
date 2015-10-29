package org.lskk.lumen.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import groovy.lang.Closure;
import groovy.transform.CompileStatic;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import java.util.Arrays;

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Repository
public class AgentRepository extends AbstractCrudRepository<AgentSocialConfig, String> {
    public AgentRepository() {
        final String pattern = "file:config/agent/*.json";
        log.info("Searching \"{}\" in \"{}\"...", pattern, System.getProperty("user.dir"));
        final ObjectMapper mapper = new ObjectMapper();
        final Resource[] resources = new PathMatchingResourcePatternResolver(((AgentRepository) org.lskk.lumen.socmed.AgentRepository).getClassLoader()).getResources(pattern);
        agents = FluentIterable.from(Arrays.asList(resources)).filter(DefaultGroovyMethods.asType(new Closure<Object>(this, this) {
            public Object doCall(Resource it) {
                return !FilenameUtils.getBaseName(it.getFilename()).contains(".");
            }

        }, Predicate.class < Resource.class)).transform(DefaultGroovyMethods.asType(new Closure<AgentSocialConfig>(this, this) {
            public AgentSocialConfig doCall(Resource it) {
                return mapper.readValue(it.getURL(), AgentSocialConfig.class);
            }

        }, Function.class < Resource.class)).uniqueIndex(DefaultGroovyMethods.asType(new Closure<String>(this, this) {
            public String doCall(AgentSocialConfig it) {
                return it.getId();
            }

        }, Function.class < AgentSocialConfig.class));
        log.info("Loaded {} agents: {}", new Object[]{agents.size(), agents.keySet()});
    }

    @Override
    public boolean exists(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(AgentSocialConfig entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Iterable<? extends AgentSocialConfig> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException();
    }

    private static final Logger log = LoggerFactory.getLogger(AgentRepository.class);
    protected final ImmutableMap<String, AgentSocialConfig> agents;
}

package id.ac.itb.lumen.social

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.FluentIterable
import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Repository
class AgentRepository extends AbstractCrudRepository<AgentSocialConfig, String> {

    private static final Logger log = LoggerFactory.getLogger(AgentRepository.class)
    protected final ImmutableMap<String, AgentSocialConfig> agents

    AgentRepository() {
        final mapper = new ObjectMapper()
        final resources = new PathMatchingResourcePatternResolver(AgentRepository.classLoader).getResources("config/agent/*.json")
        agents = FluentIterable.from(Arrays.asList(resources))
            .filter({ Resource it -> !FilenameUtils.getBaseName(it.getFilename()).contains(".") } as Predicate<Resource>)
            .transform({ Resource it -> mapper.readValue(it.getURL(), AgentSocialConfig.class) } as Function<Resource, AgentSocialConfig>)
            .uniqueIndex({ AgentSocialConfig it -> it.id } as Function<AgentSocialConfig, String>)
        log.info("Loaded {} agents: {}", [agents.size(), agents.keySet()] as Object[])
    }

//    @Override
//    def <S extends AgentSocialConfig> S save(S entity) {
//        throw new UnsupportedOperationException()
//    }

    // FIXME: how to override type-safely?
//    @Override
//    def Object save(Object entity) {
//        throw new UnsupportedOperationException()
//    }

    @Override
    def <S extends AgentSocialConfig> Iterable<S> save(Iterable<S> entities) {
        throw new UnsupportedOperationException()
    }

    @Override
    AgentSocialConfig findOne(String id) {
        agents.get(id)
    }

    @Override
    boolean exists(String id) {
        throw new UnsupportedOperationException()
    }

    @Override
    Iterable<AgentSocialConfig> findAll() {
        agents.values()
    }

    @Override
    Iterable<AgentSocialConfig> findAll(Iterable<String> strings) {
        throw new UnsupportedOperationException()
    }

    @Override
    long count() {
        throw new UnsupportedOperationException()
    }

    @Override
    void delete(String id) {
        throw new UnsupportedOperationException()
    }

    @Override
    void delete(AgentSocialConfig entity) {
        throw new UnsupportedOperationException()
    }

    @Override
    void delete(Iterable<? extends AgentSocialConfig> entities) {
        throw new UnsupportedOperationException()
    }

    @Override
    void deleteAll() {
        throw new UnsupportedOperationException()
    }
    
}

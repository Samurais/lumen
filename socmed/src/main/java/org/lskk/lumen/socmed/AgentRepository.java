package org.lskk.lumen.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.auth.AccessToken;
import facebook4j.auth.OAuthAuthorization;
import facebook4j.conf.PropertyConfiguration;
import groovy.lang.Closure;
import groovy.transform.CompileStatic;
import org.apache.camel.component.facebook.config.FacebookConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by ceefour on 1/19/15.
 */
@Repository
@DependsOn("proxyConfig")
public class AgentRepository extends AbstractCrudRepository<AgentSocialConfig, String> {
    private static final Logger log = LoggerFactory.getLogger(AgentRepository.class);
    protected final ImmutableMap<String, AgentSocialConfig> agents;

    public AgentRepository() throws IOException {
        final String pattern = "file:config/agent/*.AgentSocialConfig.json";
        log.info("Searching \"{}\" in \"{}\"...", pattern, System.getProperty("user.dir"));
        final ObjectMapper mapper = new ObjectMapper();
        final Resource[] resources = new PathMatchingResourcePatternResolver(AgentRepository.class.getClassLoader()).getResources(pattern);
        agents = FluentIterable.from(Arrays.asList(resources))
                .filter(it -> FilenameUtils.getBaseName(it.getFilename()).endsWith(".AgentSocialConfig"))
                .transform(it -> {
                    try {
                        return mapper.readValue(it.getURL(), AgentSocialConfig.class);
                    } catch (IOException e) {
                        throw new SocmedException(e, "Cannot read JSON from %s", it);
                    }
                })
                .uniqueIndex(AgentSocialConfig::getId);
        log.info("Loaded {} agents: {}", agents.size(), agents.keySet());
    }

    @PostConstruct
    public void init() {
//        agents.values().stream()
//                .filter(agent -> agent.getFacebookSys().getFacebookAccessToken() != null)
//                .forEach(agent -> {
//                    final Properties props = new Properties();
//                    props.put(PropertyConfiguration.OAUTH_APP_ID, agent.getFacebookSys().getFacebookAppId());
//                    props.put(PropertyConfiguration.OAUTH_APP_SECRET, agent.getFacebookSys().getFacebookAppSecret());
//                    final Facebook facebook = new FacebookFactory(new PropertyConfiguration(props)).getInstance(new AccessToken(agent.getFacebookSys().getFacebookAccessToken()));
//                    try {
//                        log.debug("Extending {}'s Facebook access token...", agent.getId());
//                        final AccessToken extendedToken = facebook.extendTokenExpiration(facebook.getOAuthAccessToken().getToken());
//                        log.info("New {} Facebook access token: {}", agent.getId(), extendedToken.getToken());
//                        agent.getFacebookSys().setFacebookAccessToken(extendedToken.getToken());
//                    } catch (FacebookException e) {
//                        throw new SocmedException(e, "Cannot extend %s's Facebook access token", agent.getId());
//                    }
//        });
    }

    @Override
    public <S extends AgentSocialConfig> S save(S entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S extends AgentSocialConfig> Iterable<S> save(Iterable<S> entities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AgentSocialConfig findOne(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<AgentSocialConfig> findAll() {
        return agents.values();
    }

    @Override
    public Iterable<AgentSocialConfig> findAll(Iterable<String> strings) {
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

}

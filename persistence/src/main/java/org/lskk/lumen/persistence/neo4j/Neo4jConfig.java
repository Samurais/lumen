package org.lskk.lumen.persistence.neo4j;

import com.google.common.collect.ImmutableMap;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.server.RemoteServer;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Created by ceefour on 1/22/15.
 */
@Configuration
@EnableNeo4jRepositories(basePackageClasses = Neo4jConfig.class)
@EnableTransactionManagement
@Profile("spring-data-neo4j")
public class Neo4jConfig extends Neo4jConfiguration {
//    @Bean @TaxonomyRelated
//    public GraphDatabaseService graphDatabaseService() {
//        return new GraphDatabaseFactory().newEmbeddedDatabase(
//                new File(env.getRequiredProperty("workspaceDir"), "lumen/taxonomy.neo4j"));
//    }

    @Inject
    protected Environment env;

    @Override
    public Neo4jServer neo4jServer() {
        return new RemoteServer(env.getRequiredProperty("spring.neo4j.url"));
    }

    @Override
    public SessionFactory getSessionFactory() {
        return new SessionFactory(Neo4jConfig.class.getPackage().getName());
    }

    // needed for session in view in web-applications
    @Bean
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Session getSession() throws Exception {
        return super.getSession();
    }

    @Configuration
    public static class IndexesConfig {

        @Inject
        private SessionFactory sessionFactory;
        @Inject
        private Environment env;

        @PostConstruct
        public void init() {
            final Session session = sessionFactory.openSession(env.getRequiredProperty("spring.neo4j.url"));
            final Neo4jTemplate neo4j = new Neo4jTemplate(session);
            neo4j.query("CREATE INDEX ON :schema_Thing(_partition)", ImmutableMap.of());
            neo4j.query("CREATE CONSTRAINT ON (thing:schema_Thing) ASSERT thing.nn IS UNIQUE", ImmutableMap.of());
            neo4j.query("CREATE INDEX ON :schema_Thing(prefLabel)", ImmutableMap.of());
        }

    }

    //    // TODO: https://github.com/spring-projects/spring-data-neo4j/issues/227
//    @Configuration
//    @Profile("spring-data-neo4j")
//    //@EnableNeo4jRepositories(basePackageClasses = Person.class)
//    public static class Neo4jMoreConfig extends Neo4jConfiguration {
//        public Neo4jMoreConfig() {
//            setBasePackage(Person.class.getPackage().getName());
//        }
//    }
}

package org.lskk.lumen.persistence;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.config.Neo4jConfiguration;

import javax.inject.Inject;
import java.io.File;

/**
 * Created by ceefour on 1/22/15.
 */
@Configuration
@Profile("neo4j-dbs")
public class Neo4jConfig {
    @Bean @TaxonomyRelated
    public GraphDatabaseService graphDatabaseService() {
        return new GraphDatabaseFactory().newEmbeddedDatabase(
                new File(env.getRequiredProperty("workspaceDir"), "lumen/taxonomy.neo4j"));
    }

    @Inject
    protected Environment env;

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

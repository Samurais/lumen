package id.ac.itb.lumen.persistence

import groovy.transform.CompileStatic
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.data.neo4j.config.EnableNeo4jRepositories
import org.springframework.data.neo4j.config.Neo4jConfiguration
import org.springframework.data.neo4j.support.Neo4jTemplate

import javax.inject.Inject

/**
 * Created by ceefour on 1/22/15.
 */
@CompileStatic
@Configuration
@EnableNeo4jRepositories(basePackageClasses = Person)
@Profile('!batchinserter')
class Neo4jConfig {

    @Inject
    protected Environment env

    @Bean
    GraphDatabaseService graphDatabaseService() {
        new GraphDatabaseFactory().newEmbeddedDatabase(
                env.getRequiredProperty('neo4j.path'))
    }

    // TODO: https://github.com/spring-projects/spring-data-neo4j/issues/227
    @Configuration
    @Profile('!batchinserter')
    static class Neo4jMoreConfig extends Neo4jConfiguration {

        Neo4jMoreConfig() {
            setBasePackage(Person.class.package.name)
        }

    }

}

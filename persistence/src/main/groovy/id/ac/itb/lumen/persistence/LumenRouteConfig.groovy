package id.ac.itb.lumen.persistence

import com.google.common.base.Preconditions
import com.google.common.collect.FluentIterable
import com.google.common.collect.ImmutableList
import com.rabbitmq.client.ConnectionFactory
import groovy.transform.CompileStatic
import id.ac.itb.lumen.core.Channel
import org.apache.camel.builder.RouteBuilder
import org.neo4j.graphdb.Node
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.neo4j.conversion.Result
import org.springframework.data.neo4j.support.Neo4jTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

import javax.inject.Inject

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Configuration
@Profile('daemon')
class LumenRouteConfig {

    private static final Logger log = LoggerFactory.getLogger(LumenRouteConfig.class)

//    @Inject
//    protected PersonRepository personRepo
    @Inject
    protected ToJson toJson
    @Inject
    protected PlatformTransactionManager txMgr
    @Inject
    protected Neo4jTemplate neo4j

    @Bean
    ConnectionFactory amqpConnFactory() {
      final connFactory = new ConnectionFactory()
      connFactory.host = 'localhost'
      connFactory.username = 'guest'
      connFactory.password = 'guest'
      return connFactory
    }

    @Bean
    def RouteBuilder personFindAllRouteBuilder() {
        log.info('Initializing personFindAll RouteBuilder')
        new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from('rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=' + Channel.PERSISTENCE_FACT.key('arkan'))
                        .to('log:IN.persistence-fact?showHeaders=true&showAll=true&multiline=true')
                        .process {
                    final findAllQuery = toJson.mapper.readValue(it.in.body as byte[], FindAllQuery)
                    def classAbbrevRef = Optional.ofNullable(RdfUtils.PREFIX_MAP.abbreviate(findAllQuery.classRef))
                        .orElse(findAllQuery.classRef)
                    final resources = new TransactionTemplate(txMgr).execute {
                        final cypher = 'MATCH (e:Resource) -[:rdf_type*]-> (:Resource {href: {classAbbrevRef}}) RETURN e LIMIT 25'
                        log.debug('Querying using {}: {}', [classAbbrevRef: classAbbrevRef], cypher)
                        final rs = neo4j.query(cypher, [classAbbrevRef: classAbbrevRef] as Map<String, Object>)
                                .collect { it['e'] as Node }.toList()
                        log.info('{} rows in result set for {}: {}', rs.size(), classAbbrevRef, rs)
                        new Resources<>(rs.collect {
                            final indexedRes = new IndexedResource()
                            indexedRes.href = it.getProperty('href')
                            indexedRes.prefLabel = it.getProperty('prefLabel')
                            indexedRes.isPreferredMeaningOf = it.getProperty('isPreferredMeaningOf')
                            indexedRes
                        })
                    }
                    it.out.body = resources
                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                }.bean(toJson)
                    // https://issues.apache.org/jira/browse/CAMEL-8270
                    .to('rabbitmq://localhost/dummy?connectionFactory=#amqpConnFactory&autoDelete=false')
                    .to('log:OUT.persistence-fact?showAll=true&multiline=true')
            }
        }
    }

}

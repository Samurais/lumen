package id.ac.itb.lumen.persistence

import com.google.common.base.Preconditions
import com.google.common.collect.FluentIterable
import com.google.common.collect.ImmutableList
import com.rabbitmq.client.ConnectionFactory
import groovy.transform.CompileStatic
import id.ac.itb.lumen.core.Channel
import org.apache.camel.builder.RouteBuilder
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
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
                    try {
                        final inBodyJson = toJson.mapper.readTree(it.in.body as byte[])
                        switch (inBodyJson.path('@type').asText()) {
                            case 'FindAllQuery':
                                final findAllQuery = toJson.mapper.convertValue(inBodyJson, FindAllQuery)
                                def classAbbrevRef = Optional.ofNullable(RdfUtils.PREFIX_MAP.abbreviate(findAllQuery.classRef))
                                        .orElse(findAllQuery.classRef)
                                final resources = new TransactionTemplate(txMgr).execute {
                                    final cypher = 'MATCH (e:Resource) -[:rdf_type*]-> (:Resource {href: {classAbbrevRef}}) RETURN e LIMIT 25'
                                    log.debug('Querying using {}: {}', [classAbbrevRef: classAbbrevRef], cypher)
                                    final rs = neo4j.query(cypher, [classAbbrevRef: classAbbrevRef] as Map<String, Object>)
                                    try {
                                        final rsList = rs.collect { it['e'] as Node }.toList()
                                        log.debug('{} rows in result set for {}: {}', rsList.size(), classAbbrevRef, rsList)
                                        new Resources<>(rsList.collect {
                                            final indexedRes = new IndexedResource()
                                            indexedRes.href = it.getProperty('href')
                                            indexedRes.prefLabel = it.getProperty('prefLabel')
                                            indexedRes.isPreferredMeaningOf = it.getProperty('isPreferredMeaningOf')
                                            indexedRes
                                        })
                                    } finally {
                                        rs.finish()
                                    }
                                }
                                it.out.body = resources
                                break;
                            case 'CypherQuery':
                                final cypherQuery = toJson.mapper.convertValue(inBodyJson, CypherQuery)
                                final resources = new TransactionTemplate(txMgr).execute {
                                    log.debug('Querying using {}: {}', cypherQuery.parameters, cypherQuery.query)
                                    final rs = neo4j.query(cypherQuery.query, cypherQuery.parameters)
                                    try {
                                        final rsList = ImmutableList.copyOf(rs)
                                        log.debug('{} rows in result set: {}', rsList.size(), rsList)
                                        new Resources<>(rsList.collect {
                                            new ResultRow(it.collect { k, v ->
                                                if (v instanceof Node) {
                                                    new ResultCell(k, new Neo4jNode(v as Node))
                                                } else if (v instanceof Relationship) {
                                                    new ResultCell(k, new Neo4jRelationship(v as Relationship))
                                                } else {
                                                    new ResultCell(k, v)
                                                }
                                            })
                                        })
                                    } finally {
                                        rs.finish()
                                    }
                                }
                                it.out.body = resources
                                break;
                            default:
                                throw new Exception('Unknown JSON message: ' + inBodyJson);
                        }
                    } catch (Exception e) {
                        log.error("Cannot process: " + it.in.body, e)
                        it.out.body = new Error(e)
                    }

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

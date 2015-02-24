package id.ac.itb.lumen.persistence

import com.google.common.base.Preconditions
import com.google.common.collect.FluentIterable
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.rabbitmq.client.ConnectionFactory
import groovy.transform.CompileStatic
import id.ac.itb.lumen.core.BatteryState
import id.ac.itb.lumen.core.Channel
import id.ac.itb.lumen.core.ImageObject
import id.ac.itb.lumen.core.ImageObjectLegacy
import id.ac.itb.lumen.core.JointSetLegacy
import id.ac.itb.lumen.core.JointState
import id.ac.itb.lumen.core.SonarState
import id.ac.itb.lumen.core.TactileSetLegacy
import id.ac.itb.lumen.core.TactileState
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.data.neo4j.conversion.Result
import org.springframework.data.neo4j.support.Neo4jTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

import javax.inject.Inject
import java.util.concurrent.TimeUnit

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Configuration
@Profile('daemon')
class LumenRouteConfig {

    private static final Logger log = LoggerFactory.getLogger(LumenRouteConfig.class)

    @Inject
    protected Environment env
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
      connFactory.host = env.getRequiredProperty('amqp.host')
      connFactory.username = env.getRequiredProperty('amqp.username')
      connFactory.password = env.getRequiredProperty('amqp.password')
      return connFactory
    }

    @Bean
    def RouteBuilder factRouteBuilder() {
        log.info('Initializing fact RouteBuilder')
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
                                    final cypher = 'MATCH (e:Resource) -[:rdf_type*]-> (:Resource {href: {classAbbrevRef}}) RETURN e LIMIT {itemsPerPage}'
                                    final params = [classAbbrevRef: classAbbrevRef, itemsPerPage: findAllQuery.itemsPerPage] as Map<String, Object>
                                    log.debug('Querying using {}: {}', params, cypher)
                                    final rs = neo4j.query(cypher, params)
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

    @Bean
    def RouteBuilder journalRouteBuilder() {
        log.info('Initializing journal RouteBuilder')
        final mediaUploadPrefix = env.getRequiredProperty('media.upload.prefix')
        final mediaDownloadPrefix = env.getRequiredProperty('media.download.prefix')
        new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from('rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=' + Channel.PERSISTENCE_JOURNAL.key('arkan'))
                        .to('log:IN.persistence-journal?showHeaders=true&showAll=true&multiline=true')
                        .process {
                    try {
                        final inBodyJson = toJson.mapper.readTree(it.in.body as byte[])
                        switch (inBodyJson.path('@type').asText()) {
                            case 'JournalImageQuery':
                                final journalImageQuery = toJson.mapper.convertValue(inBodyJson, JournalImageQuery)
                                final resources = new TransactionTemplate(txMgr).execute {
                                    final params = [
                                        maxDateCreated: journalImageQuery.maxDateCreated,
                                        itemsPerPage: journalImageQuery.itemsPerPage
                                    ] as Map<String, Object>
                                    final cypher = 'MATCH (n:JournalImageObject) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}'
                                    log.debug('Querying: {} {}', cypher, params)
                                    final rs = neo4j.query(cypher, params)
                                    try {
                                        final rsList = rs.collect { it['n'] as Node }.toList()
                                        log.debug('{} rows in result set: {}', rsList.size(), rsList)
                                        new Resources<>(rsList.collect {
                                            final imageObject = new ImageObject()
                                            imageObject.dateCreated = Optional.ofNullable(it.getProperty('dateCreated')).map {
                                                new DateTime(it)
                                            }.orElse(null)
                                            imageObject.datePublished = Optional.ofNullable(it.getProperty('datePublished')).map {
                                                new DateTime(it)
                                            }.orElse(null)
                                            imageObject.dateModified = Optional.ofNullable(it.getProperty('dateModified')).map {
                                                new DateTime(it)
                                            }.orElse(null)
                                            imageObject.uploadDate = Optional.ofNullable(it.getProperty('uploadDate')).map {
                                                new DateTime(it)
                                            }.orElse(null)
                                            final upContentUrl = it.getProperty('contentUrl') as String
                                            imageObject.contentUrl = upContentUrl.replace(mediaUploadPrefix, mediaDownloadPrefix)
                                            imageObject.contentSize = it.getProperty('contentSize') as Long
                                            imageObject.contentType = it.getProperty('contentType')
                                            imageObject.name = it.getProperty('name')
                                            imageObject
                                        })
                                    } finally {
                                        rs.finish()
                                    }
                                }
                                it.out.body = resources
                                break;
                            case 'JournalJointQuery':
                                final journalJointQuery = toJson.mapper.convertValue(inBodyJson, JournalJointQuery)
                                final resources = new TransactionTemplate(txMgr).execute {
                                    final params = [
                                        maxDateCreated: journalJointQuery.maxDateCreated,
                                        itemsPerPage: journalJointQuery.itemsPerPage
                                    ] as Map<String, Object>
                                    final cypher = 'MATCH (n:JournalJoint) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}'
                                    log.debug('Querying: {} {}', cypher, params)
                                    final rs = neo4j.query(cypher, params)
                                    try {
                                        final rsList = rs.collect { it['n'] as Node }.toList()
                                        log.debug('{} rows in result set: {}', rsList.size(), rsList)
                                        new Resources<>(rsList.collect {
                                            final jointState = new JointState()
                                            jointState.dateCreated = Optional.ofNullable(it.getProperty('dateCreated')).map { new DateTime(it) }.orElse(null)
                                            jointState.name = it.getProperty('name')
                                            jointState.angle = it.getProperty('angle') as Double
                                            jointState.stiffness = it.getProperty('stiffness') as Double
                                            jointState
                                        })
                                    } finally {
                                        rs.finish()
                                    }
                                }
                                it.out.body = resources
                                break;
                            case 'JournalTactileQuery':
                                final journalTactileQuery = toJson.mapper.convertValue(inBodyJson, JournalTactileQuery)
                                final resources = new TransactionTemplate(txMgr).execute {
                                    final params = [
                                        maxDateCreated: journalTactileQuery.maxDateCreated,
                                        itemsPerPage: journalTactileQuery.itemsPerPage
                                    ] as Map<String, Object>
                                    final cypher = 'MATCH (n:JournalTactile) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}'
                                    log.debug('Querying: {} {}', cypher, params)
                                    final rs = neo4j.query(cypher, params)
                                    try {
                                        final rsList = rs.collect { it['n'] as Node }.toList()
                                        log.debug('{} rows in result set: {}', rsList.size(), rsList)
                                        new Resources<>(rsList.collect {
                                            final tactileState = new TactileState()
                                            tactileState.dateCreated = Optional.ofNullable(it.getProperty('dateCreated')).map { new DateTime(it) }.orElse(null)
                                            tactileState.name = it.getProperty('name')
                                            tactileState.value = it.getProperty('value') as Double
                                            tactileState
                                        })
                                    } finally {
                                        rs.finish()
                                    }
                                }
                                it.out.body = resources
                                break;
                            case 'JournalSonarQuery':
                                final journalSonarQuery = toJson.mapper.convertValue(inBodyJson, JournalSonarQuery)
                                final resources = new TransactionTemplate(txMgr).execute {
                                    final params = [
                                        maxDateCreated: journalSonarQuery.maxDateCreated,
                                        itemsPerPage: journalSonarQuery.itemsPerPage
                                    ] as Map<String, Object>
                                    final cypher = 'MATCH (n:JournalSonarState) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}'
                                    log.debug('Querying: {} {}', cypher, params)
                                    final rs = neo4j.query(cypher, params)
                                    try {
                                        final rsList = rs.collect { it['n'] as Node }.toList()
                                        log.debug('{} rows in result set: {}', rsList.size(), rsList)
                                        new Resources<>(rsList.collect {
                                            final sonarState = new SonarState()
                                            sonarState.dateCreated = Optional.ofNullable(it.getProperty('dateCreated')).map { new DateTime(it) }.orElse(null)
                                            sonarState.leftSensor = it.getProperty('leftSensor') as Double
                                            sonarState.rightSensor = it.getProperty('rightSensor') as Double
                                            sonarState
                                        })
                                    } finally {
                                        rs.finish()
                                    }
                                }
                                it.out.body = resources
                                break;
                            case 'JournalBatteryQuery':
                                final journalBatteryQuery = toJson.mapper.convertValue(inBodyJson, JournalBatteryQuery)
                                final resources = new TransactionTemplate(txMgr).execute {
                                    final params = [
                                        maxDateCreated: journalBatteryQuery.maxDateCreated,
                                        itemsPerPage: journalBatteryQuery.itemsPerPage
                                    ] as Map<String, Object>
                                    final cypher = 'MATCH (n:JournalBatteryState) WHERE n.dateCreated <= {maxDateCreated} RETURN n ORDER BY n.dateCreated DESC LIMIT {itemsPerPage}'
                                    log.debug('Querying: {} {}', cypher, params)
                                    final rs = neo4j.query(cypher, params)
                                    try {
                                        final rsList = rs.collect { it['n'] as Node }.toList()
                                        log.debug('{} rows in result set: {}', rsList.size(), rsList)
                                        new Resources<>(rsList.collect {
                                            final batteryState = new BatteryState()
                                            batteryState.dateCreated = Optional.ofNullable(it.getProperty('dateCreated')).map { new DateTime(it) }.orElse(null)
                                            batteryState.percentage = it.getProperty('percentage') as Double
                                            batteryState.isCharging = it.getProperty('isCharging') as Boolean
                                            batteryState.isPlugged = it.getProperty('isPlugged') as Boolean
                                            batteryState
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
                    .to('log:OUT.persistence-journal?showAll=true&multiline=true')
            }
        }
    }

    @Bean
    def RouteBuilder imageRouteBuilder() {
        log.info('Initializing image RouteBuilder')

        final mediaUploadPath = new File(env.getRequiredProperty('media.upload.path'))
        mediaUploadPath.mkdirs()
        final mediaUploadPrefix = env.getRequiredProperty('media.upload.prefix')
        final extensionMap = [
                'image/jpeg': 'jpg',
                'image/png': 'png',
                'image/gif': 'gif',
                'image/bmp': 'bmp'
        ]

        new TransactionTemplate(txMgr).execute { tx ->
            neo4j.query('CREATE INDEX ON :JournalImageObject(dateCreated)', [:]).finish()
        }

        new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from('rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.image')
                        .sample(1, TimeUnit.SECONDS)
                        .to('log:IN.avatar.NAO.data.image?showHeaders=true&showAll=true&multiline=true')
                        .process { Exchange it ->
                    try {
                        final inBodyJson = toJson.mapper.readTree(it.in.body as byte[])
                        final imageObject = toJson.mapper.convertValue(inBodyJson, ImageObjectLegacy)
                        new TransactionTemplate(txMgr).execute { tx ->
                            final now = new DateTime() // FIXME: NaoServer should send ISO formatted timestamp
                            final props = [
                                    name: imageObject.name,
                                    contentType: imageObject.contentType,
                                    contentSize: imageObject.contentSize,
                                    url: imageObject.url,
                                    uploadDate: now.toString(),
                                    dateCreated: now.toString(),
                                    datePublished: now.toString(),
                                    dateModified: now.toString()
                            ] as Map<String, Object>
                            final contentType = Preconditions.checkNotNull(imageObject.contentType,
                                    'ImageObject.contentType must be specified')
                            final upContentUrl = imageObject.contentUrl
                            if (upContentUrl != null && upContentUrl.startsWith('data:')) {
                                final base64 = StringUtils.substringAfter(upContentUrl, ",")
                                final content = Base64.decodeBase64(base64)
                                final fileName = UUID.randomUUID().toString() + '.' + extensionMap[contentType]
                                final file = new File(mediaUploadPath, fileName)
                                log.debug('Writing {} ImageObject to {} ...', contentType, file)
                                FileUtils.writeByteArrayToFile(file, content)
                                props['contentUrl'] = mediaUploadPrefix + fileName
                            } else {
                                props['contentUrl'] = upContentUrl
                            }
                            final node = neo4j.createNode(props, ['JournalImageObject'])
                            log.debug('Created JournalImageObject {} from {} {}', node, imageObject.name, now)
                            it.out.body = node.getId()
                        }
                    } catch (Exception e) {
                        log.error("Cannot process: " + it.in.body, e)
                        it.out.body = new Error(e)
                    }

//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                }.bean(toJson)
                    // https://issues.apache.org/jira/browse/CAMEL-8270
                    //.to('rabbitmq://localhost/dummy?connectionFactory=#amqpConnFactory&autoDelete=false')
                    .to('log:OUT.avatar.NAO.data.image?showAll=true&multiline=true')
            }
        }
    }

    @Bean
    def RouteBuilder sonarRouteBuilder() {
        log.info('Initializing sonar RouteBuilder')

        new TransactionTemplate(txMgr).execute { tx ->
            neo4j.query('CREATE INDEX ON :JournalSonarState(dateCreated)', [:]).finish()
        }

        new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from('rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.sonar')
                        .sample(1, TimeUnit.SECONDS)
                        .to('log:IN.avatar.NAO.data.sonar?showHeaders=true&showAll=true&multiline=true')
                        .process { Exchange it ->
                    try {
                        final inBodyJson = toJson.mapper.readTree(it.in.body as byte[])
                        final sonarState = toJson.mapper.convertValue(inBodyJson, SonarState)
                        new TransactionTemplate(txMgr).execute { tx ->
                            final now = new DateTime()
                            final props = [
                                leftSensor: sonarState.leftSensor,
                                rightSensor: sonarState.rightSensor,
                                dateCreated: now.toString()
                            ] as Map<String, Object>
                            final node = neo4j.createNode(props, ['JournalSonarState'])
                            log.debug('Created JournalSonarState {} from {}', node, props)
                            it.out.body = node.getId()
                        }
                    } catch (Exception e) {
                        log.error("Cannot process: " + it.in.body, e)
                        it.out.body = new Error(e)
                    }

//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                }.bean(toJson)
                    // https://issues.apache.org/jira/browse/CAMEL-8270
                    //.to('rabbitmq://localhost/dummy?connectionFactory=#amqpConnFactory&autoDelete=false')
                    .to('log:OUT.avatar.NAO.data.sonar?showAll=true&multiline=true')
            }
        }
    }

    @Bean
    def RouteBuilder batteryRouteBuilder() {
        log.info('Initializing battery RouteBuilder')

        new TransactionTemplate(txMgr).execute { tx ->
            neo4j.query('CREATE INDEX ON :JournalBatteryState(dateCreated)', [:]).finish()
        }

        new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from('rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.battery')
                        .sample(1, TimeUnit.SECONDS)
                        .to('log:IN.avatar.NAO.data.battery?showHeaders=true&showAll=true&multiline=true')
                        .process { Exchange it ->
                    try {
                        final inBodyJson = toJson.mapper.readTree(it.in.body as byte[])
                        final batteryState = toJson.mapper.convertValue(inBodyJson, BatteryState)
                        new TransactionTemplate(txMgr).execute { tx ->
                            final now = new DateTime()
                            final props = [
                                percentage: batteryState.percentage,
                                isPlugged: batteryState.isPlugged,
                                isCharging: batteryState.isCharging,
                                dateCreated: now.toString()
                            ] as Map<String, Object>
                            final node = neo4j.createNode(props, ['JournalBatteryState'])
                            log.debug('Created JournalBatteryState {} from {}', node, props)
                            it.out.body = node.getId()
                        }
                    } catch (Exception e) {
                        log.error("Cannot process: " + it.in.body, e)
                        it.out.body = new Error(e)
                    }

//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                }.bean(toJson)
                    // https://issues.apache.org/jira/browse/CAMEL-8270
                    //.to('rabbitmq://localhost/dummy?connectionFactory=#amqpConnFactory&autoDelete=false')
                    .to('log:OUT.avatar.NAO.data.battery?showAll=true&multiline=true')
            }
        }
    }

    @Bean
    def RouteBuilder jointRouteBuilder() {
        log.info('Initializing joint RouteBuilder')

        new TransactionTemplate(txMgr).execute { tx ->
            neo4j.query('CREATE INDEX ON :JournalJoint(name)', [:]).finish()
            neo4j.query('CREATE INDEX ON :JournalJoint(dateCreated)', [:]).finish()
        }

        new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from('rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.joint')
                        .sample(1, TimeUnit.SECONDS)
                        .to('log:IN.avatar.NAO.data.joint?showHeaders=true&showAll=true&multiline=true')
                        .process { Exchange it ->
                    try {
                        final inBodyJson = toJson.mapper.readTree(it.in.body as byte[])
                        final jointSet = toJson.mapper.convertValue(inBodyJson, JointSetLegacy)
                        new TransactionTemplate(txMgr).execute { tx ->
                            final now = new DateTime()
                            final List<Node> nodes = []
                            jointSet.names.eachWithIndex { jointName, i ->
                                final props = [
                                    name: jointName,
                                    angle: jointSet.angles[i],
                                    stiffness: jointSet.angles[i],
                                    dateCreated: now.toString()
                                ] as Map<String, Object>
                                final node = neo4j.createNode(props, ['JournalJoint'])
                                nodes.add(node)
                            }
                            log.debug('Created {} JournalJoint(s) {} from {}', nodes, jointSet)
                            it.out.body = nodes.collect { it.getId() }
                        }
                    } catch (Exception e) {
                        log.error("Cannot process: " + it.in.body, e)
                        it.out.body = new Error(e)
                    }

//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                }.bean(toJson)
                    // https://issues.apache.org/jira/browse/CAMEL-8270
                    //.to('rabbitmq://localhost/dummy?connectionFactory=#amqpConnFactory&autoDelete=false')
                    .to('log:OUT.avatar.NAO.data.joint?showAll=true&multiline=true')
            }
        }
    }

    @Bean
    def RouteBuilder tactileRouteBuilder() {
        log.info('Initializing tactile RouteBuilder')

        new TransactionTemplate(txMgr).execute { tx ->
            neo4j.query('CREATE INDEX ON :JournalTactile(name)', [:]).finish()
            neo4j.query('CREATE INDEX ON :JournalTactile(dateCreated)', [:]).finish()
        }

        new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from('rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=avatar.NAO.data.tactile')
                        .sample(1, TimeUnit.SECONDS)
                        .to('log:IN.avatar.NAO.data.tactile?showHeaders=true&showAll=true&multiline=true')
                        .process { Exchange it ->
                    try {
                        final inBodyJson = toJson.mapper.readTree(it.in.body as byte[])
                        final tactileSet = toJson.mapper.convertValue(inBodyJson, TactileSetLegacy)
                        new TransactionTemplate(txMgr).execute { tx ->
                            final now = new DateTime()
                            final List<Node> nodes = []
                            tactileSet.names.eachWithIndex { tactileName, i ->
                                final props = [
                                    name: tactileName,
                                    value: tactileSet.values[i],
                                    dateCreated: now.toString()
                                ] as Map<String, Object>
                                final node = neo4j.createNode(props, ['JournalTactile'])
                                nodes.add(node)
                            }
                            log.debug('Created {} JournalTactile(s) {} from {}', nodes, tactileSet)
                            it.out.body = nodes.collect { it.getId() }
                        }
                    } catch (Exception e) {
                        log.error("Cannot process: " + it.in.body, e)
                        it.out.body = new Error(e)
                    }

//                    it.out.headers['rabbitmq.ROUTING_KEY'] = Preconditions.checkNotNull(it.in.headers['rabbitmq.REPLY_TO'],
//                            '"rabbitmq.REPLY_TO" header must be specified, found headers: %s', it.in.headers)
//                    it.out.headers['rabbitmq.EXCHANGE_NAME'] = ''
                }.bean(toJson)
                    // https://issues.apache.org/jira/browse/CAMEL-8270
                    //.to('rabbitmq://localhost/dummy?connectionFactory=#amqpConnFactory&autoDelete=false')
                    .to('log:OUT.avatar.NAO.data.tactile?showAll=true&multiline=true')
            }
        }
    }

}

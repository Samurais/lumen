package id.ac.itb.lumen.persistence

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import com.hp.hpl.jena.graph.Node_Literal
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra
import groovy.transform.CompileStatic
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.cypherdsl.CypherQuery
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.unsafe.batchinsert.BatchInserters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Profile
import org.springframework.data.neo4j.support.Neo4jTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.TransactionTemplate

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.nio.charset.StandardCharsets
import java.text.NumberFormat

/**
 * Note: GC limit exceeded may happen even during clearing Neo4j DB.
 * At least -Xmx4g is recommended
 */
@CompileStatic
@SpringBootApplication
@EnableTransactionManagement
@Profile('import')
class LumenPersistenceImportApp implements CommandLineRunner {

    protected static final Logger log = LoggerFactory.getLogger(LumenPersistenceImportApp)
    static final LUMEN_NAMESPACE = 'http://lumen.lskk.ee.itb.ac.id/resource/'
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.ENGLISH)
    
    @Inject
    protected PersonRepository personRepo
//    @Inject
//    protected PlatformTransactionManager txMgr
//    @Inject
//    protected Neo4jTemplate neo4j
    @Inject
    protected GraphDatabaseService db

    protected TransactionTemplate txTemplate
    protected ExecutionEngine exec

    @PostConstruct
    void init() {
//        txTemplate = new TransactionTemplate(txMgr)
        exec = new ExecutionEngine(db)
    }

    void importFile(File file) {
        log.info('Importing {} ({} KiB) ...', file, NUMBER.format((file.length() / 1024) as long))
        long importeds = 0
        long readCount = 0
        new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
                .withReader { Reader buf ->
            final csv = new CSVReader(buf, '\t' as char, CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)
            csv.withCloseable {
//                def tx = txMgr.getTransaction(txTemplate)
                def tx = db.beginTx()

                def line = csv.readNext()
                while (line != null) {
                    final factId = Strings.emptyToNull(line[0])
                    final factHref = factId != null ? 'yago:' + factId.replaceAll('[<>]', '') : null
                    final factCypher = factHref != null ? 'f: {factHref}' : ''
                    final subject = line[1]
                    final subjectHref = 'yago:' + subject.replaceAll('[<>]', '')
                    final String property = line[2]
                    final relName
                    if (property.startsWith('<')) {
                        relName = 'yago_' + property.replaceAll('[<>]', '')
                    } else {
                        relName = property.replace(':', '_')
                    }

                    final resOrLiteral = line[3]
                    if (resOrLiteral.startsWith('<')) { // Object is Resource
                        final objectHref = 'yago:' + resOrLiteral.replaceAll('[<>]', '')
//                        log.trace('{} -[:{}]-> {}    # {}', subjectHref, relName, objectHref, factId)

                        final merge = """
    MERGE (subj:Resource {href: {subjectHref}})
    MERGE (obj:Resource {href: {objectHref}})
    CREATE (subj) -[:$relName { $factCypher }]-> (obj)
    """
//                        final subj = neo4j.query(merge,
//                                [subjectHref: subjectHref,
//                                 objectHref: objectHref,
//                                 factHref: factHref] as Map<String, Object>)
//                        log.trace('{} -[:{}]-> {}    # {}   » {}', subjectHref, relName, objectHref, factId, subj.single())
                        exec.execute(merge,
                                [subjectHref: subjectHref,
                                 objectHref: objectHref,
                                 factHref: factHref] as Map<String, Object>)
                        importeds++
                    } else { // Object is Literal
                        final literal = (Node_Literal) NodeFactoryExtra.parseNode(resOrLiteral)
                        final literalValue
                        if ([XSDDatatype.XSDdate, XSDDatatype.XSDdateTime].contains(literal.literalDatatype)) {
                            literalValue = literal.toString(null, false)
                        } else {
                            literalValue = literal.literalValue.toString()
                        }

                        // to save space & overhead: only import languageless, English, and Indonesian, otherwise ignore
                        if (literal.literalLanguage == null || ['eng', 'ind'].contains(literal.literalLanguage)) {
                            final langCypher = literal.literalLanguage != null ? ", l: {literalLanguage}" : ''
                            //                        log.trace('{} -[:{}]-> {}    # {}', subjectHref, relName, literal, factId)

                            final merge
                            if ('rdfs:label'.equals(property)) {
                                // to save space & speed-up import, YAGO labels are only set as node properties but not as relationships
                                // meaning no factIds as well
                                final labelProperty = literal.literalLanguage != null ? "label_${literal.literalLanguage}" : 'label'
//                                merge = """
//    MERGE (subj:Resource {href: {subjectHref}})
//        ON MATCH SET subj.$labelProperty = {literalValue}
//        ON CREATE SET subj.$labelProperty = {literalValue}
//    CREATE (subj) -[:$relName { $factCypher }]-> (obj:Literal {t: {dataType}, v: {literalValue} $langCypher })
//    RETURN subj
//    """
                                merge = """
    MERGE (subj:Resource {href: {subjectHref}})
        ON MATCH SET subj.$labelProperty = {literalValue}
        ON CREATE SET subj.$labelProperty = {literalValue}
    """
                            } else {
                                merge = """
    MERGE (subj:Resource {href: {subjectHref}})
    CREATE (subj) -[:$relName { $factCypher }]-> (obj:Literal {t: {dataType}, v: {literalValue} $langCypher })
    """
                            }
                            //                        final subj = neo4j.query(merge,
                            //                                [subjectHref: subjectHref,
                            //                                 dataType: literal.literalDatatypeURI, literalValue: literalValue,
                            //                                 literalLanguage: literal.literalLanguage,
                            //                                 factHref: factHref] as Map<String, Object>)
                            //                        log.trace('{} -[:{}]-> {}    # {}   » {}', subjectHref, relName, literal, factId, subj.single())
                            exec.execute(merge,
                                    [subjectHref: subjectHref,
                                     dataType   : literal.literalDatatypeURI, literalValue: literalValue,
                                     literalLanguage: literal.literalLanguage,
                                     factHref   : factHref] as Map<String, Object>)
                            importeds++
                        }
                    }

                    readCount++
                    if (readCount % 10000 == 0) {
                        log.info('Imported {} out of {} statements from {}',
                                NUMBER.format(importeds), NUMBER.format(readCount), file)
//                        txMgr.commit(tx)
//                        tx = txMgr.getTransaction(txTemplate)
                        tx.success()
                        tx.close()
                        tx = db.beginTx()
                    }

                    line = csv.readNext()
                }
//                txMgr.commit(tx)
                tx.success()
                tx.close()
                log.info('Completed importing {} out of {} statements from {}',
                        NUMBER.format(importeds), NUMBER.format(readCount), file)
            }
        }
    }
    
    @Override
    void run(String... args) throws Exception {

        // PLEASE just delete lumen's neo4j data directory instead: ~/lumen_lumen_dev/neo4j
//        log.info('Purging Neo4j database...')
//        txTemplate.execute {
//            neo4j.query('MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r', [:])
//        }
//        log.info('Purged Neo4j database')

//        txTemplate.execute {
//            neo4j.query('CREATE CONSTRAINT ON (e:Resource) ASSERT e.href IS UNIQUE', [:])
//            // since Literal indexes don't scale, we put labels as node properties then index them
//            neo4j.query('CREATE INDEX ON :Resource(label)', [:])
//            neo4j.query('CREATE INDEX ON :Resource(label_eng)', [:])
//            neo4j.query('CREATE INDEX ON :Resource(label_ind)', [:])
//            // won't scale on Yago2s size, need to use ElasticSearch
////            neo4j.query('CREATE INDEX ON :Literal(t)', [:])
////            neo4j.query('CREATE INDEX ON :Literal(v)', [:])
////            neo4j.query('CREATE INDEX ON :Literal(l)', [:])
//        }
        def tx = db.beginTx()
        try {
            // href must be indexed due to MERGE
            exec.execute('CREATE CONSTRAINT ON (e:Resource) ASSERT e.href IS UNIQUE', [:])
            // since Literal indexes don't scale, we put labels as node properties then index them AFTER import finished
//            exec.execute('CREATE INDEX ON :Resource(label)', [:])
//            exec.execute('CREATE INDEX ON :Resource(label_eng)', [:])
//            exec.execute('CREATE INDEX ON :Resource(label_ind)', [:])
            // won't scale on Yago2s size, need to use ElasticSearch
//            neo4j.query('CREATE INDEX ON :Literal(t)', [:])
//            neo4j.query('CREATE INDEX ON :Literal(v)', [:])
//            neo4j.query('CREATE INDEX ON :Literal(l)', [:])
            tx.success()
        } finally {
            tx.close()
        }
        log.info('Ensured constraints and indexes')

        Preconditions.checkArgument(args.length >= 1, 'yago2s-directory argument is required' as Object)

//        final file = new File(args[0], 'yagoSimpleTaxonomy.tsv')
        // step 1: Labels (1.1 GB: 15.372.313 lines)
        importFile(new File(args[0], 'yagoLabels.tsv'))
        // step 2: Literal Facts
//        importFile(new File(args[0], 'yagoLiteralFacts.tsv'))

//        final resolver = new PathMatchingResourcePatternResolver(new FileSystemResourceLoader())
//        final resources = resolver.getResources('file:' + args[0] + '/*.tsv')
//        log.info('Importing {} TSVs: {}', resources.length, resources)
//        resources.each { Resource it -> importFile(it.file) }
    }

    static void main(String[] args) {
        new SpringApplicationBuilder(LumenPersistenceImportApp)
                .profiles('import')
                .run(args)
    }
}

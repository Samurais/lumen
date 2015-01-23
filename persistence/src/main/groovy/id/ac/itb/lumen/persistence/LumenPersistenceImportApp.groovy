package id.ac.itb.lumen.persistence

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import com.hp.hpl.jena.graph.Node_Literal
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra
import groovy.transform.CompileStatic
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
    
    @Inject
    protected PersonRepository personRepo
    @Inject
    protected PlatformTransactionManager txMgr
    @Inject
    protected Neo4jTemplate neo4j

    protected TransactionTemplate txTemplate

    @PostConstruct
    void init() {
        txTemplate = new TransactionTemplate(txMgr)
    }

    void importFile(File file) {
        log.info('Importing {} ({} KiB) ...', file, (file.length() / 1024) as long)
        long importeds = 0
        new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
                .withReader { Reader buf ->
            final csv = new CSVReader(buf, '\t' as char, CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)
            csv.withCloseable {
                def tx = txMgr.getTransaction(txTemplate)

                def line = csv.readNext()
                while (line != null) {
                    final factId = Strings.emptyToNull(line[0])
                    final factUri = factId != null ? 'http://yago-knowledge.org/resource/' + factId.replaceAll('[<>]', '') : null
                    final factUriCypher = factUri != null ? 'factUri: {factUri}' : ''
                    final subject = line[1]
                    final subjectHref = 'yago:' + subject.replaceAll('[<>]', '')
                    final String property = line[2]
                    final relName
                    if (property.startsWith('<')) {
                        relName = 'yago_' + property.replaceAll('[<>]', '')
                    } else {
                        relName = property.replace(':', '_')
                    }

                    final objectOrLiteral = line[3]
                    if (objectOrLiteral.startsWith('<')) { // Object is Resource
                        final objectHref = 'yago:' + objectOrLiteral.replaceAll('[<>]', '')
                        final objectQName = 'yago:' + objectOrLiteral.replaceAll('[<>]', '')
                        log.trace('{} -[:{}]-> {}    # {}', subjectHref, relName, objectHref, factId)

                        final merge = """
    MERGE (subj:Resource {href: {subjectHref}})
    MERGE (obj:Resource {href: {objectUri}})
    MERGE (subj) -[:$relName { $factUriCypher }]-> (obj)
    RETURN subj
    """
                        final subj = neo4j.query(merge,
                                [subjectHref: subjectHref,
                                 objectHref: objectHref,
                                 factUri: factUri] as Map<String, Object>)
                        log.trace('{} -[:{}]-> {}    # {}   » {}', subjectHref, relName, objectHref, factId, subj.single())
                    } else { // Object is Literal
                        final literal = (Node_Literal) NodeFactoryExtra.parseNode(objectOrLiteral)
                        final literalValue
                        if ([XSDDatatype.XSDdate, XSDDatatype.XSDdateTime].contains(literal.literalDatatype)) {
                            literalValue = literal.toString(null, false)
                        } else {
                            literalValue = literal.literalValue.toString()
                        }
                        final langCypher = literal.literalLanguage != null ? ", l: {literalLanguage}" : ''
                        log.trace('{} -[:{}]-> {}    # {}', subjectHref, relName, literal, factId)

                        final merge
                        if (property.equals('rdfs:label')) {
                            final labelProperty = literal.literalLanguage != null ? "label_${literal.literalLanguage}" : 'label'
                            merge = """
MERGE (subj:Resource {href: {subjectHref}})
    ON MATCH SET subj.$labelProperty = {literalValue}
    ON CREATE SET subj.$labelProperty = {literalValue}
MERGE (subj) -[:$relName { $factUriCypher }]-> (obj:Literal {t: {dataType}, v: {literalValue} $langCypher })
RETURN subj
"""
                        } else {
                            merge = """
MERGE (subj:Resource {href: {subjectHref}})
MERGE (subj) -[:$relName { $factUriCypher }]-> (obj:Literal {t: {dataType}, v: {literalValue} $langCypher })
RETURN subj
"""
                        }
                        final subj = neo4j.query(merge,
                                [subjectHref: subjectHref,
                                 dataType: literal.literalDatatypeURI, literalValue: literalValue,
                                 literalLanguage: literal.literalLanguage,
                                 factUri: factUri] as Map<String, Object>)
                        log.trace('{} -[:{}]-> {}    # {}   » {}', subjectHref, relName, literal, factId, subj.single())
                    }
                    importeds++
                    if (importeds % 1000 == 0) {
                        log.info('Committing {} statements from {}', importeds, file)
                        txMgr.commit(tx)
                        tx = txMgr.getTransaction(txTemplate)
                    }

                    line = csv.readNext()
                }
                txMgr.commit(tx)
                log.info('Completed importing {} statements from {}', importeds, file)
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

        txTemplate.execute {
            neo4j.query('CREATE CONSTRAINT ON (e:Resource) ASSERT e.href IS UNIQUE', [:])
            // since Literal indexes don't scale, we put labels as node properties then index them
            neo4j.query('CREATE INDEX ON :Resource(label)', [:])
            neo4j.query('CREATE INDEX ON :Resource(label_eng)', [:])
            neo4j.query('CREATE INDEX ON :Resource(label_ind)', [:])
            // won't scale on Yago2s size, need to use ElasticSearch
//            neo4j.query('CREATE INDEX ON :Literal(t)', [:])
//            neo4j.query('CREATE INDEX ON :Literal(v)', [:])
//            neo4j.query('CREATE INDEX ON :Literal(l)', [:])
        }
        log.info('Ensured constraints and indexes')

        Preconditions.checkArgument(args.length >= 1, 'yago2s-directory argument is required' as Object)

//        final file = new File(args[0], 'yagoSimpleTaxonomy.tsv')
        importFile(new File(args[0], 'yagoLabels.tsv'))
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

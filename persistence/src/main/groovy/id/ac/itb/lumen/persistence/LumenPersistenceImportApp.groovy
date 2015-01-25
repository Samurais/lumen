package id.ac.itb.lumen.persistence

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterators
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import com.hp.hpl.jena.graph.Node_Literal
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra
import groovy.transform.CompileStatic
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.DynamicLabel
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.TransactionTemplate

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.nio.charset.StandardCharsets
import java.text.NumberFormat

/**
 * Only -Xmx1g is needed because it's assumed you'll use tmpfs
 */
@CompileStatic
@SpringBootApplication
@EnableTransactionManagement
@Profile('import')
class LumenPersistenceImportApp implements CommandLineRunner {

    protected static final Logger log = LoggerFactory.getLogger(LumenPersistenceImportApp)
    static final LUMEN_NAMESPACE = 'http://lumen.lskk.ee.itb.ac.id/resource/'
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.ENGLISH)
    
//    @Inject
//    protected PlatformTransactionManager txMgr
//    @Inject
//    protected Neo4jTemplate neo4j
    @Inject
    protected GraphDatabaseService db

    protected TransactionTemplate txTemplate
    protected ExecutionEngine exec

    /**
     * Relates a subject resource with an object resource.
     */
    protected static class ImportFact {
        String subjectHref
        String relName
        Map<String, ?> relProps
        String objectHref

        ImportFact(String subjectHref, String relName, Map<String, ?> relProps, String objectHref) {
            this.subjectHref = subjectHref
            this.relName = relName
            this.relProps = relProps
            this.objectHref = objectHref
        }
    }

    /**
     * Relates a subject resource with a {@code Literal} node.
     */
    protected static class ImportLiteral {
        String subjectHref
        String relName
        Map<String, ?> relProps
        Map<String, ?> literalProps

        ImportLiteral(String subjectHref, String relName, Map<String, ?> relProps, Map<String, ?> literalProps) {
            this.subjectHref = subjectHref
            this.relName = relName
            this.relProps = relProps
            this.literalProps = literalProps
        }
    }

    protected static class ImportBatch {
        /**
         * Subjects and objects to MERGE, i.e. <code>MERGE (subj:Resource {href: {subjectHref}})</code>
         * where the binding is the same as href but with ':' replaced with '_'
         */
        private BiMap<String, Integer> resourceHrefs = HashBiMap.create(1000)
        List<ImportFact> facts = new ArrayList<>(1000)
        List<ImportLiteral> literals = new ArrayList<>(1000)
        int ops = 0

        int addResourceHref(String resourceHref) {
            final existing = resourceHrefs.get(resourceHref)
            if (existing != null) {
                return existing
            } else {
                final pos = resourceHrefs.size()
                resourceHrefs.put(resourceHref, pos)
                pos
            }
        }

        void incOps() {
            ops++
        }

        void exec(Transaction tx, ExecutionEngine exec) {
            if (resourceHrefs.isEmpty()) {
                log.info('Not committing empty ImportBatch')
                return
            }

            log.trace('Merging {} resources, {} facts, and {} literals...', resourceHrefs.size(), facts.size(), literals.size())
            def cypher = ''
            final Map<String, Object> params = [:]
            cypher += "// Resources: ${resourceHrefs.size()}\n"
            resourceHrefs.each { String k, Integer v ->
                cypher += 'MERGE (res' + v + ':Resource {href: {res' + v + 'href}})\n'
                params['res' + v + 'href'] = k
            }
            cypher += '\n'
            cypher += "// Literals: ${literals.size()}\n"
            literals.eachWithIndex { it, idx ->
                cypher += 'CREATE (res' + resourceHrefs[it.subjectHref] + ') -[:' + it.relName + ' {literalRel' + (idx as String) + '}]-> (:Literal {literal' + (idx as String) + '})\n'
                params['literalRel' + (idx as String)] = it.relProps
                params['literal' + (idx as String)] = it.literalProps
            }
            cypher += '\n'
            cypher += "// Facts: ${facts.size()}\n"
            facts.eachWithIndex { it, idx ->
                cypher += 'CREATE (res' + resourceHrefs[it.subjectHref] + ') -[:' + it.relName + ' {factRel' + (idx as String) + '}]-> (res' + resourceHrefs[it.objectHref] + ')\n'
                params['factRel' + (idx as String)] = it.relProps
            }
            log.trace('Cypher: {} » Params: {}', cypher, params)
            exec.execute(cypher, params)
            log.trace('Merged {} resources, {} facts, and {} literals', resourceHrefs.size(), facts.size(), literals.size())
        }

    }

    @PostConstruct
    void init() {
//        txTemplate = new TransactionTemplate(txMgr)
        exec = new ExecutionEngine(db)
    }

    void importFile(File file) {
        log.info('Importing {} ({} KiB) ...', file, NUMBER.format((file.length() / 1024) as long))

        final resourceLabel = DynamicLabel.label('Resource')
        final literalLabel = DynamicLabel.label('Literal')

        long lastMilestone = System.currentTimeMillis()
        long importeds = 0
        long lastImporteds = 0
        long readCount = 0
        long commits = 0
        def batch = new ImportBatch()
        new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 1024 * 1024)
                .withReader { Reader buf ->
            final csv = new CSVReader(buf, '\t' as char, CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)
            csv.withCloseable {
//                def tx = txMgr.getTransaction(txTemplate)
                def tx = db.beginTx()

                def line = csv.readNext()
                while (line != null) {
                    final factId = Strings.emptyToNull(line[0])
                    final factHref = factId != null ? 'yago:' + factId.replaceAll('[<>]', '') : null
                    final relProps = factHref != null ? [f: factHref] : [:]
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

//                        final merge = """
//    MERGE (subj:Resource {href: {subjectHref}})
//    MERGE (obj:Resource {href: {objectHref}})
//    CREATE (subj) -[:$relName {relProps}]-> (obj)
//    """

//                        final subj = neo4j.query(merge,
//                                [subjectHref: subjectHref,
//                                 objectHref: objectHref,
//                                 factHref: factHref] as Map<String, Object>)
//                        log.trace('{} -[:{}]-> {}    # {}   » {}', subjectHref, relName, objectHref, factId, subj.single())
//                        exec.execute(merge,
//                                [subjectHref: subjectHref,
//                                 objectHref: objectHref,
//                                 relProps: relProps] as Map<String, Object>)

                        batch.addResourceHref(subjectHref)
                        batch.addResourceHref(objectHref)
                        batch.facts += new ImportFact(subjectHref, relName, relProps, objectHref)
                        batch.incOps()
                        importeds++
                    } else { // Object is Literal
                        final literal = (Node_Literal) NodeFactoryExtra.parseNode(resOrLiteral)
                        final datatypeRef = RdfUtils.abbrevDatatype(literal)
                        final literalValue
                        // Remember, literal.literalDatatype can be null
                        if ([XSDDatatype.XSDdate, XSDDatatype.XSDdateTime].contains(literal.literalDatatype)) {
                            literalValue = literal.toString(null, false)
                        } else {
                            literalValue = literal.literalValue.toString()
                        }

                        if (['rdfs:label', 'skos:prefLabel', 'yago:isPreferredMeaningOf'].contains(property)) {
                            // to save space & speed-up import, skos:prefLabel and yago:isPreferredMeaningOf
                            // are only set as node properties but not as relationships meaning no factIds as well.
                            // Processed ONLY IF node didn't exist or node has no prefLabel.
                            // It will *not* create `rdfs_label` relationship.
                            Node subjectNode = Iterators.getNext(db.findNodesByLabelAndProperty(resourceLabel, 'href', subjectHref).iterator(), null)
                            if (subjectNode == null || !subjectNode.hasProperty('prefLabel')) {
                                if (subjectNode == null) {
                                    subjectNode = db.createNode(resourceLabel)
                                    subjectNode.properties['href'] = subjectHref
                                }
                                if (['rdfs:label', 'skos:prefLabel'].equals(property)) {
                                    subjectNode.properties['prefLabel'] = literalValue
                                } else if ('yago:isPreferredMeaningOf'.equals(property)) {
                                    subjectNode.properties['isPreferredMeaningOf'] = literalValue
                                }
                                importeds++
                            } // otherwise ignored
                        } else {
                            final literalProps = [v: literalValue]
                            if (datatypeRef != null) {
                                literalProps['t'] = datatypeRef
                            }
                            if (literal.literalLanguage != null) {
                                literalProps['l'] = literal.literalLanguage
                            }

                            final merge = """
MERGE (subj:Resource {href: {subjectHref}})
CREATE (subj) -[:$relName {relProps}]-> (lit:Literal {literalProps})
"""
                            final params = [subjectHref: subjectHref,
                                            relProps   : relProps,
                                            literalProps: literalProps] as Map<String, Object>

                            try {
                                //                        final subj = neo4j.query(merge,
                                //                                [subjectHref: subjectHref,
                                //                                 dataType: literal.literalDatatypeURI, literalValue: literalValue,
                                //                                 literalLanguage: literal.literalLanguage,
                                //                                 factHref: factHref] as Map<String, Object>)
                                //                        log.trace('{} -[:{}]-> {}    # {}   » {}', subjectHref, relName, literal, factId, subj.single())
//                                exec.execute(merge, params)

//                                Node subjectNode = Iterators.getNext(db.findNodesByLabelAndProperty(resourceLabel, 'href', subjectHref).iterator(), null)
//                                if (subjectNode == null) {
//                                    subjectNode = db.createNode(resourceLabel)
//                                    subjectNode.properties['href'] = subjectHref
//                                }
//                                Node literalNode = db.createNode(literalLabel)
//                                literalNode.properties.putAll(literalProps)
//                                final literalRel = subjectNode.createRelationshipTo(literalNode, DynamicRelationshipType.withName(relName))
//                                literalRel.properties.putAll(relProps)

                                batch.addResourceHref(subjectHref)
                                batch.literals += new ImportLiteral(subjectHref, relName, relProps, literalProps)
                                batch.incOps()

                                importeds++
                            } catch (Exception e) {
                                throw new RuntimeException('Cannot execute: «' + merge + '» params: ' + params, e)
                            }
                        }
                    }

                    readCount++
                    if (batch.ops >= 20) {
                        batch.exec(tx, exec)
                        batch = new ImportBatch()
                    }

                    if (importeds % 10000 == 0 && lastImporteds != importeds) {
//                        txMgr.commit(tx)
//                        tx = txMgr.getTransaction(txTemplate)
                        tx.success()
                        tx.close()

                        tx = db.beginTx()
                        lastImporteds = importeds
                        commits++

                        if (importeds % 10000 == 0) {
                            final rate = NUMBER.format(10000f * 1000f / (System.currentTimeMillis() - lastMilestone))
                            lastMilestone = System.currentTimeMillis()
                            log.info('{} commits so far: {} out of {} statements ({}/s) from {}',
                                    commits, NUMBER.format(importeds), NUMBER.format(readCount), rate, file)
                        }
                    }

                    line = csv.readNext()
                }

                log.info('Finalizing batch then commit...')
//                txMgr.commit(tx)
                batch.exec(tx, exec)
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

        Preconditions.checkArgument(args.length >= 1, 'yago2s file argument is required' as Object)

//        final file = new File(args[0], 'yagoSimpleTaxonomy.tsv')
        // step 1: Labels (1.1 GB: 15.372.313 lines)
//        importFile(new File(args[0], 'yagoLabels.tsv'))
        // step 2: Literal Facts
//        importFile(new File(args[0], 'yagoLiteralFacts.tsv'))

        // step 3 and beyond
        importFile(new File(args[0]))

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

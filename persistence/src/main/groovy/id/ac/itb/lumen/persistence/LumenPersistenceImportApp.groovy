package id.ac.itb.lumen.persistence

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.Iterators
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import com.hp.hpl.jena.graph.Node_Literal
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra
import groovy.transform.CompileStatic
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.DynamicLabel
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Profile

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Only -Xmx1g is needed because it's assumed you'll use tmpfs
 */
@CompileStatic
@SpringBootApplication
@Profile('import')
class LumenPersistenceImportApp implements CommandLineRunner {

    protected static final Logger log = LoggerFactory.getLogger(LumenPersistenceImportApp)
    static final LUMEN_NAMESPACE = 'http://lumen.lskk.ee.itb.ac.id/resource/'
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.ENGLISH)
    /**
     * For tmpfs/SSD, set multithreaded to true
     */
    protected static final boolean multithreaded = true
    /**
     * Number of import ops per batch. Should be 10-50.
     */
    protected static final int opRate = 20
    /**
     * Number of ops per transaction commit.
     */
    protected static final int commitRate = multithreaded ? 10000 : 100
    
    @Inject
    protected GraphDatabaseService db

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
        private BiMap<String, Integer> resourceHrefs = HashBiMap.create(50)
        List<ImportFact> facts = new ArrayList<>(50)
        List<ImportLiteral> literals = new ArrayList<>(50)
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
        AtomicLong commits = new AtomicLong(0l)
        def batch = new ImportBatch()
        new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 1024 * 1024)
                .withReader { Reader buf ->
            final csv = new CSVReader(buf, '\t' as char, CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)
            csv.withCloseable {
//                def tx = txMgr.getTransaction(txTemplate)
                def tx = db.beginTx()
                // spare threads for the main thread and Neo4j's transaction-write thread
                def executor = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors() > 2 ? Runtime.runtime.availableProcessors() - 2 : 1)

                def line = csv.readNext()
                while (line != null) {
                    final factId = Strings.emptyToNull(line[0])
                    final factHref = factId != null ? 'yago:' + factId.replaceAll('[<>]', '') : null
                    final relProps = factHref != null ? [f: factHref] : [:]
                    final subject = line[1]
                    final String subjectHref
                    if (subject.startsWith('<')) {
                        subjectHref = 'yago:' + subject.replaceAll('[<>]', '')
                    } else {
                        subjectHref = subject
                    }
                    final String property = line[2]
                    final relName
                    if (property.startsWith('<')) {
                        relName = 'yago_' + property.replaceAll('[<>]', '')
                    } else {
                        relName = property.replace(':', '_')
                    }

                    final resOrLiteral = line[3]
                    if (!resOrLiteral.startsWith('"')) { // Object is Resource
                        // https://issues.apache.org/jira/browse/JENA-862
                        final String objectHref
                        if (resOrLiteral.startsWith('<')) {
                            objectHref = 'yago:' + resOrLiteral.replaceAll('[<>]', '')
                        } else {
                            objectHref = resOrLiteral
                        }

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
                        final literal = (Node_Literal) NodeFactoryExtra.parseNode(resOrLiteral, RdfUtils.PREFIX_MAP)
                        final datatypeRef = RdfUtils.abbrevDatatype(literal)
                        final literalValue
                        // Remember, literal.literalDatatype can be null (which means it's string)
                        if (literal.literalDatatype == null) {
                            literalValue = literal.getLiteral().lexicalForm
                        } else if ([XSDDatatype.XSDnonNegativeInteger, XSDDatatype.XSDbyte, XSDDatatype.XSDint, XSDDatatype.XSDinteger,
                                XSDDatatype.XSDlong, XSDDatatype.XSDnegativeInteger, XSDDatatype.XSDnonPositiveInteger,
                                XSDDatatype.XSDshort,
                                XSDDatatype.XSDunsignedByte, XSDDatatype.XSDunsignedInt, XSDDatatype.XSDunsignedLong, XSDDatatype.XSDunsignedShort,
                                XSDDatatype.XSDboolean,
                                XSDDatatype.XSDdouble, XSDDatatype.XSDfloat, XSDDatatype].contains(literal.literalDatatype)) {
                            literalValue = literal.literalValue
                        } else if (XSDDatatype.XSDdecimal.equals(literal.literalDatatype)) {
                            literalValue = literal.literalValue as Double
                        } else if ([XSDDatatype.XSDdate, XSDDatatype.XSDdateTime, XSDDatatype.XSDtime].contains(literal.literalDatatype)) {
                            // Esp. XSDDatatype.XSDdate, XSDDatatype.XSDdateTime, XSDDatatype.XSDtime cannot use literalValue due to
                            // its sometimes incomplete representation
                            literalValue = literal.getLiteral().lexicalForm
                        } else {
                            // parse first, so we can get proper literal type for degrees, m^2, etc.
                            try {
                                literalValue = Long.parseLong(literal.getLiteral().lexicalForm)
                            } catch (NumberFormatException e) {
                                try {
                                    literalValue = Double.parseDouble(literal.getLiteral().lexicalForm)
                                } catch (NumberFormatException e2) {
                                    literalValue = literal.getLiteral().lexicalForm
                                }
                            }
                        }

                        if (['rdfs:label', 'skos:prefLabel', '<isPreferredMeaningOf>'].contains(property)) {
                            // to save space & speed-up import, skos:prefLabel and yago:isPreferredMeaningOf
                            // are only set as node properties but not as relationships meaning no factIds as well.
                            // Processed ONLY IF node didn't exist or node has no prefLabel.
                            // It will *not* create `rdfs_label` relationship.
                            Node subjectGraphNode = Iterators.getNext(db.findNodesByLabelAndProperty(resourceLabel, 'href', subjectHref).iterator(), null)
                            if (subjectGraphNode == null || !subjectGraphNode.hasProperty('prefLabel')) {
                                if (subjectGraphNode == null) {
                                    subjectGraphNode = db.createNode(resourceLabel)
                                    subjectGraphNode.properties['href'] = subjectHref
                                }
                                if (['rdfs:label', 'skos:prefLabel'].equals(property)) {
                                    subjectGraphNode.properties['prefLabel'] = literalValue
                                } else if ('<isPreferredMeaningOf>'.equals(property)) {
                                    subjectGraphNode.properties['isPreferredMeaningOf'] = literalValue
                                }
                                importeds++
                            } // otherwise ignored
                        } else {
                            final literalProps = [v: literalValue]
                            if (!Strings.isNullOrEmpty(datatypeRef)) {
                                literalProps['t'] = datatypeRef
                            }
                            if (!Strings.isNullOrEmpty(literal.literalLanguage)) {
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
                    if (batch.ops >= opRate) { // only 20-50 can get ~3000/s
                        // SSD/tmpfs has better performance with batch+transaction per-thread,
                        // however HDD gives 19/s with that, and is better with several cyphers in a normal transaction
                        if (multithreaded) {
                            final toExec = batch
                            executor.submit {
                                tx = db.beginTx()
                                try {
                                    toExec.exec(tx, exec)
                                    tx.success()
                                    commits.incrementAndGet()
                                } finally {
                                    tx.close()
                                }
                            }
                        } else {
                            batch.exec(tx, exec)
                        }

                        batch = new ImportBatch()
                    }

                    if (importeds % commitRate == 0 && lastImporteds != importeds) {
                        // Need to avoid Java heap spinning out of control due to too many queue in executor
                        log.debug('Flushing batches for {} statements...', NUMBER.format(importeds))
                        executor.shutdown()
                        executor.awaitTermination(1, TimeUnit.DAYS)

                        if (!multithreaded) {
                            log.debug('Committing for {} statements...', NUMBER.format(importeds))
                            tx.success()
                            tx.close()
                            commits.incrementAndGet()

                            // New beginning
                            tx = db.beginTx()
                        }
                        executor = Executors.newFixedThreadPool(Runtime.runtime.availableProcessors())
                        lastImporteds = importeds

                        if (importeds % 10000 == 0) {
                            final rate = NUMBER.format((10000l * 1000l / (System.currentTimeMillis() - lastMilestone)) as long)
                            lastMilestone = System.currentTimeMillis()
                            log.info('{} commits so far: {} out of {} statements ({}/s) from {}',
                                    commits.get(), NUMBER.format(importeds), NUMBER.format(readCount), rate, file)
                        }
                    }

                    line = csv.readNext()
                }

                log.info('Finalizing batch then commit...')
                executor.shutdown()
                executor.awaitTermination(1, TimeUnit.DAYS)
                executor = null
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

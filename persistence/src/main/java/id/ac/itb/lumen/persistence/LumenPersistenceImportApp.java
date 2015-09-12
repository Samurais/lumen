package id.ac.itb.lumen.persistence;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Only -Xmx1g is needed because it's assumed you'll use tmpfs
 */
@SpringBootApplication
@Profile("import")
public class LumenPersistenceImportApp implements CommandLineRunner {
    @PostConstruct
    public void init() {
//        txTemplate = new TransactionTemplate(txMgr)
        exec = new ExecutionEngine(db);
    }

    public void importFile(final File file) throws IOException, InterruptedException {
        log.info("Importing {} ({} KiB) ...", file, NUMBER.format(DefaultGroovyMethods.asType(file.length() / 1024, Long.class)));

        final Label resourceLabel = DynamicLabel.label("Resource");
        final Label literalLabel = DynamicLabel.label("Literal");

        long lastMilestone = System.currentTimeMillis();
        long importeds = 0l;
        long lastImporteds = 0l;
        long readCount = 0l;
        final AtomicLong commits = new AtomicLong(0l);
        ImportBatch batch = new ImportBatch();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), (int) 1024 * 1024)) {
            try (final CSVReader csv = new CSVReader(reader, StringGroovyMethods.asType("\t", Character.class), CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {
//                def tx = txMgr.getTransaction(txTemplate)
                Transaction tx = db.beginTx();
                // spare threads for the main thread and Neo4j's transaction-write thread
                ExecutorService executor = Executors.newFixedThreadPool((int) Runtime.getRuntime().availableProcessors() > 2 ? Runtime.getRuntime().availableProcessors() - 2 : 1);

                String[] line = csv.readNext();
                while (line != null) {
                    final String factId = Strings.emptyToNull(line[0]);
                    final String factHref = factId != null ? "yago:" + factId.replaceAll("[<>]", "") : null;
                    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(1);
                    map.put("f", factHref);
                    final LinkedHashMap<String, String> relProps = factHref != null ? map : new LinkedHashMap();
                    final String subject = line[1];
                    final String subjectHref;
                    if (subject.startsWith("<")) {
                        subjectHref = "yago:" + subject.replaceAll("[<>]", "");
                    } else {
                        subjectHref = subject;
                    }

                    final String property = line[2];
                    final Object relName;
                    if (property.startsWith("<")) {
                        relName = "yago_" + property.replaceAll("[<>]", "");
                    } else {
                        relName = property.replace(":", "_");
                    }


                    final String resOrLiteral = line[3];
                    if (!resOrLiteral.startsWith("\"")) {// Object is Resource
                        // https://issues.apache.org/jira/browse/JENA-862
                        final String objectHref;
                        if (resOrLiteral.startsWith("<")) {
                            objectHref = "yago:" + resOrLiteral.replaceAll("[<>]", "");
                        } else {
                            objectHref = resOrLiteral;
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

                        batch.addResourceHref(subjectHref);
                        batch.addResourceHref(objectHref);
                        batch.getFacts().add(new ImportFact(subjectHref, (String) relName, relProps, objectHref));
                        batch.incOps();
                        importeds++;
                    } else {// Object is Literal
                        final Node_Literal literal = (Node_Literal) NodeFactoryExtra.parseNode(resOrLiteral, RdfUtils.getPREFIX_MAP());
                        final String datatypeRef = RdfUtils.abbrevDatatype(literal);
                        Object literalValue = null;
                        // Remember, literal.literalDatatype can be null (which means it's string)
                        if (literal.getLiteralDatatype() == null) {
                            literalValue = literal.getLiteral().getLexicalForm();
                        } else if (ImmutableSet.of(XSDDatatype.XSDnonNegativeInteger, XSDDatatype.XSDbyte, XSDDatatype.XSDint,
                                XSDDatatype.XSDinteger, XSDDatatype.XSDlong, XSDDatatype.XSDnegativeInteger,
                                XSDDatatype.XSDnonPositiveInteger, XSDDatatype.XSDshort, XSDDatatype.XSDunsignedByte,
                                XSDDatatype.XSDunsignedInt, XSDDatatype.XSDunsignedLong, XSDDatatype.XSDunsignedShort,
                                XSDDatatype.XSDboolean, XSDDatatype.XSDdouble, XSDDatatype.XSDfloat)
                                    .contains(literal.getLiteralDatatype())) {
                            literalValue = literal.getLiteralValue();
                        } else if (XSDDatatype.XSDdecimal.equals(literal.getLiteralDatatype())) {
                            literalValue = Double.valueOf(literal.getLiteralValue().toString());
                        } else if (new ArrayList<XSDDatatype>(Arrays.asList(XSDDatatype.XSDdate, XSDDatatype.XSDdateTime, XSDDatatype.XSDtime)).contains(literal.getLiteralDatatype())) {
                            // Esp. XSDDatatype.XSDdate, XSDDatatype.XSDdateTime, XSDDatatype.XSDtime cannot use literalValue due to
                            // its sometimes incomplete representation
                            literalValue = literal.getLiteral().getLexicalForm();
                        } else {
                            // parse first, so we can get proper literal type for degrees, m^2, etc.
                            try {
                                literalValue = Long.parseLong(literal.getLiteral().getLexicalForm());
                            } catch (NumberFormatException e) {
                                try {
                                    literalValue = Double.parseDouble(literal.getLiteral().getLexicalForm());
                                } catch (NumberFormatException e2) {
                                    literalValue = literal.getLiteral().getLexicalForm();
                                }
                            }
                        }

                        if (ImmutableSet.of("rdfs:label", "skos:prefLabel", "<isPreferredMeaningOf>").contains(property)) {
                            // to save space & speed-up import, skos:prefLabel and yago:isPreferredMeaningOf
                            // are only set as node properties but not as relationships meaning no factIds as well.
                            // Processed ONLY IF node didn't exist or node has no prefLabel.
                            // It will *not* create `rdfs_label` relationship.
                            Node subjectGraphNode = Iterators.getNext(db.findNodesByLabelAndProperty(resourceLabel, "href", subjectHref).iterator(), null);
                            if (subjectGraphNode == null || !subjectGraphNode.hasProperty("prefLabel")) {
                                if (subjectGraphNode == null) {
                                    subjectGraphNode = db.createNode(resourceLabel);
                                    DefaultGroovyMethods.getProperties(subjectGraphNode).put("href", subjectHref);
                                }

                                if (new ArrayList<String>(Arrays.asList("rdfs:label", "skos:prefLabel")).equals(property)) {
                                    DefaultGroovyMethods.getProperties(subjectGraphNode).put("prefLabel", literalValue);
                                } else if ("<isPreferredMeaningOf>".equals(property)) {
                                    DefaultGroovyMethods.getProperties(subjectGraphNode).put("isPreferredMeaningOf", literalValue);
                                }
                                importeds++;
                            }
// otherwise ignored
                        } else {
                            LinkedHashMap<String, Object> map1 = new LinkedHashMap<String, Object>(1);
                            map1.put("v", literalValue);
                            final LinkedHashMap<String, Object> literalProps = map1;
                            if (!Strings.isNullOrEmpty(datatypeRef)) {
                                literalProps.put("t", datatypeRef);
                            }

                            if (!Strings.isNullOrEmpty(literal.getLiteralLanguage())) {
                                literalProps.put("l", literal.getLiteralLanguage());
                            }


                            final String merge = "\nMERGE (subj:Resource {href: {subjectHref}})\nCREATE (subj) -[:" + relName + " {relProps}]-> (lit:Literal {literalProps})\n";
                            final Map<String, Object> params = new HashMap<String, Object>();

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

                                batch.addResourceHref(subjectHref);
                                batch.getLiterals().add(new ImportLiteral(subjectHref, (String) relName, relProps, literalProps));
                                batch.incOps();

                                importeds++;
                            } catch (Exception e) {
                                throw new RuntimeException("Cannot execute: «" + merge + "» params: " + params, e);
                            }

                        }

                    }

                    readCount++;
                    if (batch.getOps() >= opRate) {// only 20-50 can get ~3000/s
                        // SSD/tmpfs has better performance with batch+transaction per-thread,
                        // however HDD gives 19/s with that, and is better with several cyphers in a normal transaction
                        if (multithreaded) {
                            final ImportBatch toExec = batch;
                            executor.submit(() -> {
                                try (Transaction tx1 = db.beginTx()) {
                                    toExec.exec(tx1, exec);
                                    tx1.success();
                                    return commits.incrementAndGet();
                                }
                            });
                        } else {
                            batch.exec(tx, exec);
                        }


                        batch = new ImportBatch();
                    }


                    if (importeds % commitRate == 0 && lastImporteds != importeds) {
                        // Need to avoid Java heap spinning out of control due to too many queue in executor
                        log.debug("Flushing batches for {} statements...", NUMBER.format(importeds));
                        executor.shutdown();
                        executor.awaitTermination(1, TimeUnit.DAYS);

                        if (!multithreaded) {
                            log.debug("Committing for {} statements...", NUMBER.format(importeds));
                            tx.success();
                            tx.close();
                            commits.incrementAndGet();

                            // New beginning
                            tx = db.beginTx();
                        }

                        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                        lastImporteds = importeds;

                        if (importeds % 10000 == 0) {
                            final String rate = NUMBER.format((long) (10000l * 1000l / (System.currentTimeMillis() - lastMilestone)));
                            lastMilestone = System.currentTimeMillis();
                            log.info("{} commits so far: {} out of {} statements ({}/s) from {}", NUMBER.format(commits), NUMBER.format(importeds), NUMBER.format(readCount), rate, file);
                        }

                    }

                    line = csv.readNext();
                }

                log.info("Finalizing batch then commit...");
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.DAYS);
                executor = null;
//                txMgr.commit(tx)
                batch.exec(tx, exec);
                tx.success();
                tx.close();
                log.info("Completed importing {} out of {} statements from {}", NUMBER.format(importeds), NUMBER.format(readCount), file);
            }
        }
    }

    @Override
    public void run(String... args) throws Exception {
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
        Transaction tx = db.beginTx();
        try {
            // href must be indexed due to MERGE
            exec.execute("CREATE CONSTRAINT ON (e:Resource) ASSERT e.href IS UNIQUE", new LinkedHashMap());
            // since Literal indexes don't scale, we put labels as node properties then index them AFTER import finished
//            exec.execute('CREATE INDEX ON :Resource(label)', [:])
//            exec.execute('CREATE INDEX ON :Resource(label_eng)', [:])
//            exec.execute('CREATE INDEX ON :Resource(label_ind)', [:])
            // won't scale on Yago2s size, need to use ElasticSearch
//            neo4j.query('CREATE INDEX ON :Literal(t)', [:])
//            neo4j.query('CREATE INDEX ON :Literal(v)', [:])
//            neo4j.query('CREATE INDEX ON :Literal(l)', [:])
            tx.success();
        } finally {
            tx.close();
        }

        log.info("Ensured constraints and indexes");

        Preconditions.checkArgument(args.length >= 1, "yago2s file argument(s) is required");

//        final file = new File(args[0], 'yagoSimpleTaxonomy.tsv')
        // step 1: Labels (1.1 GB: 15.372.313 lines)
//        importFile(new File(args[0], 'yagoLabels.tsv'))
        // step 2: Literal Facts
//        importFile(new File(args[0], 'yagoLiteralFacts.tsv'))

        // step 3 and beyond
        for (String arg : args) {
            importFile(new File(arg));
        }

//        final resolver = new PathMatchingResourcePatternResolver(new FileSystemResourceLoader())
//        final resources = resolver.getResources('file:' + args[0] + '/*.tsv')
//        log.info('Importing {} TSVs: {}', resources.length, resources)
//        resources.each { Resource it -> importFile(it.file) }
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(LumenPersistenceImportApp.class).profiles("import").run(args);
    }

    public static String getLUMEN_NAMESPACE() {
        return LUMEN_NAMESPACE;
    }

    protected static final Logger log = LoggerFactory.getLogger(LumenPersistenceImportApp.class);
    private static final String LUMEN_NAMESPACE = "http://lumen.lskk.ee.itb.ac.id/resource/";
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.ENGLISH);
    /**
     * For tmpfs/SSD, set multithreaded to true
     */
    protected static final boolean multithreaded = true;
    /**
     * Number of import ops per batch. Should be 10-50.
     */
    protected static final int opRate = 20;
    /**
     * Number of ops per transaction commit.
     */
    protected static final int commitRate = multithreaded ? 10000 : 100;
    @Inject
    protected GraphDatabaseService db;
    protected ExecutionEngine exec;

    /**
     * Relates a subject resource with an object resource.
     */
    protected static class ImportFact {
        public ImportFact(String subjectHref, String relName, Map<String, ?> relProps, String objectHref) {
            this.subjectHref = subjectHref;
            this.relName = relName;
            this.relProps = relProps;
            this.objectHref = objectHref;
        }

        public String getSubjectHref() {
            return subjectHref;
        }

        public void setSubjectHref(String subjectHref) {
            this.subjectHref = subjectHref;
        }

        public String getRelName() {
            return relName;
        }

        public void setRelName(String relName) {
            this.relName = relName;
        }

        public Map<String, ?> getRelProps() {
            return relProps;
        }

        public void setRelProps(Map<String, ?> relProps) {
            this.relProps = relProps;
        }

        public String getObjectHref() {
            return objectHref;
        }

        public void setObjectHref(String objectHref) {
            this.objectHref = objectHref;
        }

        private String subjectHref;
        private String relName;
        private Map<String, ?> relProps;
        private String objectHref;
    }

    /**
     * Relates a subject resource with a {@code Literal} node.
     */
    protected static class ImportLiteral {
        public ImportLiteral(String subjectHref, String relName, Map<String, ?> relProps, Map<String, ?> literalProps) {
            this.subjectHref = subjectHref;
            this.relName = relName;
            this.relProps = relProps;
            this.literalProps = literalProps;
        }

        public String getSubjectHref() {
            return subjectHref;
        }

        public void setSubjectHref(String subjectHref) {
            this.subjectHref = subjectHref;
        }

        public String getRelName() {
            return relName;
        }

        public void setRelName(String relName) {
            this.relName = relName;
        }

        public Map<String, ?> getRelProps() {
            return relProps;
        }

        public void setRelProps(Map<String, ?> relProps) {
            this.relProps = relProps;
        }

        public Map<String, ?> getLiteralProps() {
            return literalProps;
        }

        public void setLiteralProps(Map<String, ?> literalProps) {
            this.literalProps = literalProps;
        }

        private String subjectHref;
        private String relName;
        private Map<String, ?> relProps;
        private Map<String, ?> literalProps;
    }

    protected static class ImportBatch {
        public int addResourceHref(String resourceHref) {
            final Integer existing = resourceHrefs.get(resourceHref);
            if (existing != null) {
                return ((int) (existing));
            } else {
                final int pos = resourceHrefs.size();
                resourceHrefs.put(resourceHref, pos);
                return ((int) (pos));
            }

        }

        public void incOps() {
            ops = ops++;
        }

        public void exec(Transaction tx, ExecutionEngine exec) {
            if (resourceHrefs.isEmpty()) {
                log.info("Not committing empty ImportBatch");
                return;

            }

            log.trace("Merging {} resources, {} facts, and {} literals...", resourceHrefs.size(), facts.size(), literals.size());
            String cypher = "";
            final Map<String, Object> params = new LinkedHashMap<>();
            cypher += "// Resources: " + String.valueOf(resourceHrefs.size()) + "\n";
            for (Map.Entry<String, Integer> entry : resourceHrefs.entrySet()) {
                cypher += "MERGE (res" + entry.getValue() + ":Resource {href: {res" + entry.getValue() + "href}})\n";
                params.put("res" + entry.getValue() + "href", entry.getKey());
            }
            cypher += "\n";
            cypher += "// Literals: " + String.valueOf(literals.size()) + "\n";
            for (int i = 0; i < literals.size(); i++) {
                final ImportLiteral it = literals.get(i);
                cypher += "CREATE (res" + resourceHrefs.get(it.getSubjectHref()) + ") -[:" + it.getRelName() + " {literalRel" + i + "}]-> (:Literal {literal" + i + "})\n";
                params.put("literalRel" + i, ((ImportLiteral) it).getRelProps());
            }
            cypher += "\n";
            cypher += "// Facts: " + String.valueOf(facts.size()) + "\n";
            for (int i = 0; i < facts.size(); i++) {
                final ImportFact it = facts.get(i);
                cypher += "CREATE (res" + resourceHrefs.get(it.getSubjectHref()) + ") -[:" + it.getRelName() + " {factRel" + i + "}]-> (res" + resourceHrefs.get(it.getObjectHref()) + ")\n";
                params.put("factRel" + i, it.getRelProps());
            }
            log.trace("Cypher: {} » Params: {}", cypher, params);
            exec.execute(cypher, params);
            log.trace("Merged {} resources, {} facts, and {} literals", resourceHrefs.size(), facts.size(), literals.size());
        }

        public List<ImportFact> getFacts() {
            return facts;
        }

        public void setFacts(List<ImportFact> facts) {
            this.facts = facts;
        }

        public List<ImportLiteral> getLiterals() {
            return literals;
        }

        public void setLiterals(List<ImportLiteral> literals) {
            this.literals = literals;
        }

        public int getOps() {
            return ops;
        }

        public void setOps(int ops) {
            this.ops = ops;
        }

        /**
         * Subjects and objects to MERGE, i.e. <code>MERGE (subj:Resource {href: {subjectHref}})</code>
         * where the binding is the same as href but with ':' replaced with '_'
         */
        private BiMap<String, Integer> resourceHrefs = HashBiMap.create(50);
        private List<ImportFact> facts = new ArrayList<ImportFact>(50);
        private List<ImportLiteral> literals = new ArrayList<ImportLiteral>(50);
        private int ops = 0;
    }

    private static <K, V, Value extends V> Value putAt0(Map<K, V> propOwner, K key, Value value) {
        propOwner.put(key, value);
        return value;
    }
}

package org.lskk.lumen.persistence;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;

/**
 * -Xmx4g please
 */
@SpringBootApplication(exclude={//CrshAutoConfiguration.class,
        JmxAutoConfiguration.class,
        LiquibaseAutoConfiguration.class})
@Profile("importyagotaxonomy")
public class ImportYagoTaxonomyApp implements CommandLineRunner {

    protected static final Logger log = LoggerFactory.getLogger(ImportYagoTaxonomyApp.class);
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.US);
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
    private Environment env;
    private File taxonomyDbFolder;

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

        public void exec(Transaction tx, GraphDatabaseService db) {
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
            db.execute(cypher, params);
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
        private List<ImportFact> facts = new ArrayList<>(50);
        private List<ImportLiteral> literals = new ArrayList<>(50);
        private int ops = 0;
    }

    @PostConstruct
    public void init() {
        taxonomyDbFolder = new File(env.getRequiredProperty("workspaceDir"), "lumen/taxonomy.neo4j");
//        txTemplate = new TransactionTemplate(txMgr)
    }

    @Bean(destroyMethod = "shutdown")
    public GraphDatabaseService taxonomyDb() {
        return new GraphDatabaseFactory().newEmbeddedDatabase(taxonomyDbFolder.getPath());
    }

    /**
     * Create initial taxonomy type nodes from taxonomyFile.
     * @param taxonomyFile
     * @throws IOException
     * @throws InterruptedException
     */
    public Set<String> createInitialTypes(final File taxonomyFile) throws IOException, InterruptedException {
        log.info("Create initial types {} ({} KiB) ...", taxonomyFile, NUMBER.format(taxonomyFile.length() / 1024));

        final Set<String> typeHrefs = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(taxonomyFile), StandardCharsets.UTF_8), (int) 1024 * 1024)) {
            try (final CSVReader csv = new CSVReader(reader, '\t', CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {

                while (true) {
                    final String[] line = csv.readNext();
                    if (line == null) {
                        break;
                    }

                    final String property = line[2];
                    if (!"rdfs:subClassOf".equals(property)) {
                        continue;
                    }

                    final String subject = line[1];
                    final String subjectHref;
                    if (subject.startsWith("<")) {
                        subjectHref = "yago:" + subject.replaceAll("[<>]", "");
                    } else {
                        subjectHref = subject;
                    }

                    final String resOrLiteral = line[3];
                    final String objectHref;
                    if (resOrLiteral.startsWith("<")) {
                        objectHref = "yago:" + resOrLiteral.replaceAll("[<>]", "");
                    } else {
                        objectHref = resOrLiteral;
                    }

                    typeHrefs.add(subjectHref);
                    typeHrefs.add(objectHref);
                }

            }
        }


        try (final Transaction tx = taxonomyDb().beginTx()) {
            log.info("Creating {} types... {}", typeHrefs.size(), typeHrefs.stream().limit(10).toArray());

            typeHrefs.stream().forEach(it -> {
                        try (Result res = taxonomyDb().execute("CREATE (n:schema_Thing {nn: {nn}})",
                                ImmutableMap.of("nn", it))) { }
                    });
            log.info("Committing transaction...");
            tx.success();
        }

        return typeHrefs;
    }

    /**
     * Link subclasses from taxonomyFile.
     * @param taxonomyFile
     * @throws IOException
     * @throws InterruptedException
     */
    public void linkSubclasses(final File taxonomyFile) throws IOException, InterruptedException {
        log.info("Link subclasses {} ({} KiB) ...", taxonomyFile, NUMBER.format(taxonomyFile.length() / 1024));

        try (final Transaction tx = taxonomyDb().beginTx()) {

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(taxonomyFile), StandardCharsets.UTF_8), 1024 * 1024)) {
                try (final CSVReader csv = new CSVReader(reader, '\t', CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {

                    while (true) {
                        final String[] line = csv.readNext();
                        if (line == null) {
                            break;
                        }

                        final String property = line[2];
                        if (!"rdfs:subClassOf".equals(property)) {
                            continue;
                        }

                        final String subject = line[1];
                        final String subjectHref;
                        if (subject.startsWith("<")) {
                            subjectHref = "yago:" + subject.replaceAll("[<>]", "");
                        } else {
                            subjectHref = subject;
                        }

                        final String resOrLiteral = line[3];
                        final String objectHref;
                        if (resOrLiteral.startsWith("<")) {
                            objectHref = "yago:" + resOrLiteral.replaceAll("[<>]", "");
                        } else {
                            objectHref = resOrLiteral;
                        }

                        final ImmutableMap<String, Object> props = ImmutableMap.of("subclass", subjectHref, "superclass", objectHref);
                        try (Result res = taxonomyDb().execute("MATCH (subclass:schema_Thing {nn: {subclass}}), (superclass:schema_Thing {nn: {superclass}}) " +
                                "CREATE (subclass) -[:rdfs_subClassOf]-> (superclass)", props)) {}
                    }

                }
            }

            log.info("Committing transaction...");
            tx.success();
        }
    }

    /**
     * Add labels from labelsFile.
     * @param labelsFile
     * @throws IOException
     * @throws InterruptedException
     */
    public void addLabels(final Set<String> typeHrefs, final File labelsFile) throws IOException, InterruptedException {
        log.info("Add labels {} ({} KiB) ...", labelsFile, NUMBER.format(labelsFile.length() / 1024));

        try (final Transaction tx = taxonomyDb().beginTx()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(labelsFile), StandardCharsets.UTF_8), 1024 * 1024)) {
                try (final CSVReader csv = new CSVReader(reader, '\t', CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {
                    while (true) {
                        final String[] line = csv.readNext();
                        if (line == null) {
                            break;
                        }

                        final String subject = line[1];
                        final String subjectHref;
                        if (subject.startsWith("<")) {
                            subjectHref = "yago:" + subject.replaceAll("[<>]", "");
                        } else {
                            subjectHref = subject;
                        }

                        if (!typeHrefs.contains(subjectHref)) {
                            continue;
                        }

                        final String property = line[2];
                        if (ImmutableSet.of("skos:prefLabel", "<isPreferredMeaningOf>").contains(property)) {
                            final String resOrLiteral = line[3];

                            final Node_Literal literal = (Node_Literal) NodeFactoryExtra.parseNode(resOrLiteral, RdfUtils.getPREFIX_MAP());
                            final String literalStr = literal.getLiteralValue().toString();

                            final ImmutableMap<String, Object> props = ImmutableMap.of("href", subjectHref, "literalStr", literalStr);
                            if ("skos:prefLabel".equals(property)) {
                                try (Result res = taxonomyDb().execute("MATCH (n:schema_Thing {nn: {href}}) SET n.prefLabel = {literalStr}", props)) { }
                            } else if ("<isPreferredMeaningOf".equals(property)) {
                                try (Result res = taxonomyDb().execute("MATCH (n:schema_Thing {nn: {href}}) SET n.isPreferredMeaningOf = {literalStr}", props)) { }
                            }
                        }
                    }
                }
            }

            log.info("Committing transaction...");
            tx.success();
        }
    }

    @Override
    public void run(String... args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Usage: ./importyagotaxonomy YAGO3_FOLDER");

//        log.info("Purging {} ...", taxonomyDbFolder);
//        FileUtils.deleteDirectory(taxonomyDbFolder);

        try (final Transaction tx = taxonomyDb().beginTx()) {

            log.info("Ensuring constraints and indexes");
            try (Result res = taxonomyDb().execute("CREATE CONSTRAINT ON (thing:schema_Thing) ASSERT thing.nn IS UNIQUE")) {}
            try (Result res = taxonomyDb().execute("CREATE INDEX ON :schema_Thing(prefLabel)")) {}
            try (Result res = taxonomyDb().execute("CREATE INDEX ON :schema_Thing(isPreferredMeaningOf)")) {
            }

            log.info("Committing transaction...");
            tx.success();
        }

        final Set<String> typeHrefs = createInitialTypes(new File(args[0], "yagoTaxonomy.tsv"));
        linkSubclasses(new File(args[0], "yagoTaxonomy.tsv"));
        addLabels(typeHrefs, new File(args[0], "yagoLabels.tsv"));

        log.info("Done");
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(ImportYagoTaxonomyApp.class)
                .profiles("importyagotaxonomy")
                .run(args);
    }

}

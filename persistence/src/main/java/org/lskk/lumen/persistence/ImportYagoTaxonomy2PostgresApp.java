package org.lskk.lumen.persistence;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.lskk.lumen.persistence.jpa.YagoType;
import org.lskk.lumen.persistence.jpa.YagoTypeRepository;
import org.lskk.lumen.persistence.jpa.YagoLabelRepository;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PostgreSQL is used to store labels and glossary, Neo4j is used to store taxonomy hierarchy (e.g. {@link org.apache.jena.vocabulary.RDFS#subClassOf},
 * {@link org.apache.jena.vocabulary.RDFS#subPropertyOf}, {@link org.apache.jena.vocabulary.RDF#type}).
 *
 * This works together with {@link ImportYagoTaxonomy2Neo4jApp}.
 *
 * -Xmx4g please
 */
@SpringBootApplication(exclude={//CrshAutoConfiguration.class,
        JmxAutoConfiguration.class, CamelAutoConfiguration.class, GroovyTemplateAutoConfiguration.class})
@Profile("importYagoTaxonomy2PostgresApp")
public class ImportYagoTaxonomy2PostgresApp implements CommandLineRunner {

    protected static final Logger log = LoggerFactory.getLogger(ImportYagoTaxonomy2PostgresApp.class);
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.US);
    /**
     * Number of import ops per batch. Should be 10-50.
     */
    protected static final int opRate = 20;

    @Inject
    private Environment env;
    @Inject
    private YagoTypeRepository yagoEntityRepo;
    @Inject
    private YagoLabelRepository yagoLabelRepo;
    @Inject
    private PlatformTransactionManager txMgr;

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

    /**
     * Get taxonomy type nodes from taxonomyFile.
     * @param taxonomyFile
     * @throws IOException
     * @throws InterruptedException
     */
    public Set<String> getSubjectHrefsFor(File taxonomyFile) {
        log.info("Get types from {} ({} KiB) ...", taxonomyFile, NUMBER.format(taxonomyFile.length() / 1024));

        final Set<String> typeHrefs = new HashSet<>();
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

                    typeHrefs.add(subjectHref);
                    typeHrefs.add(objectHref);
                }

            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        return typeHrefs;
    }

    /**
     * Create initial taxonomy type nodes from typeHrefs.
     * @param typeHrefs
     * @throws IOException
     * @throws InterruptedException
     */
    public Set<String> createInitialTypes(Set<String> typeHrefs) {
        log.info("Creating {} types... {}", typeHrefs.size(), typeHrefs.stream().limit(10).toArray());
        yagoEntityRepo.save(typeHrefs.stream().map(typeHref -> {
            final YagoType entity = new YagoType();
            entity.setNn(typeHref);
            return entity;
        }).collect(Collectors.toList()));
        log.info("Created {} types: {}", typeHrefs.size(), typeHrefs.stream().limit(10).toArray());

        return typeHrefs;
    }

    /**
     * Link subclasses from taxonomyFile.
     * @param taxonomyFile
     * @throws IOException
     * @throws InterruptedException
     */
    public void linkSubclasses(final File taxonomyFile) {
        log.info("Link subclasses {} ({} KiB) ...", taxonomyFile, NUMBER.format(taxonomyFile.length() / 1024));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(taxonomyFile), StandardCharsets.UTF_8), 1024 * 1024)) {
            try (final CSVReader csv = new CSVReader(reader, '\t', CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {

                int addedCount = 0;
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
//                    final YagoEntity subclass = yagoEntityRepo.findOneByNn(subjectHref);
//                    final YagoEntity superclass = yagoEntityRepo.findOneByNn(objectHref);
//                    subclass.getSuperClasses().add(superclass);
//                    yagoEntityRepo.save(subclass);
                    yagoEntityRepo.addSuperClass(subjectHref, objectHref);
                    addedCount++;

                    if (addedCount % 10000 == 0) {
                        log.info("Linked {} subclasses so far...", addedCount);
                    }
                }

            }

            log.info("Linked subclasses {} ({} KiB)", taxonomyFile, NUMBER.format(taxonomyFile.length() / 1024));
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    /**
     * Add labels from labelsFile.
     * @param labelsFile
     * @throws IOException
     * @throws InterruptedException
     */
    public void addLabelsOnlyForExisting(final Set<String> typeHrefs, final File labelsFile) {
        log.info("Add labels {} ({} KiB) ...", labelsFile, NUMBER.format(labelsFile.length() / 1024));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(labelsFile), StandardCharsets.UTF_8), 1024 * 1024)) {
            try (final CSVReader csv = new CSVReader(reader, '\t', CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {
                int labelCount = 0;
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
                    if (ImmutableSet.of("skos:prefLabel", "<isPreferredMeaningOf>",
                            "<hasGivenName>", "<hasFamilyName>", "<hasGloss>", "<redirectedFrom>",
                            "rdfs:label").contains(property)) {
                        final String resOrLiteral = line[3];

                        final Node_Literal literal = (Node_Literal) NodeFactoryExtra.parseNode(resOrLiteral, RdfUtils.getPREFIX_MAP());
                        final String literalLanguage = literal.getLiteralLanguage();
                        final String literalStr = literal.getLiteralValue().toString();

                        final ImmutableMap<String, Object> props = ImmutableMap.of("href", subjectHref, "literalStr", literalStr);
                        if ("skos:prefLabel".equals(property)) {
                            yagoEntityRepo.updatePrefLabel(subjectHref, literalStr);
                            labelCount++;
                        } else if ("<isPreferredMeaningOf>".equals(property)) {
                            yagoEntityRepo.updateIsPreferredMeaningOf(subjectHref, literalStr);
                            labelCount++;
                        } else if ("<hasGivenName>".equals(property)) {
                            yagoEntityRepo.updateHasGivenName(subjectHref, literalStr);
                            labelCount++;
                        } else if ("<hasFamilyName>".equals(property)) {
                            yagoEntityRepo.updateHasFamilyName(subjectHref, literalStr);
                            labelCount++;
                        } else if ("<hasGloss>".equals(property)) {
                            yagoEntityRepo.updateHasGloss(subjectHref, literalStr);
                            labelCount++;
                        } else if ("<redirectedFrom>".equals(property)) {
                            yagoEntityRepo.updateRedirectedFrom(subjectHref, literalStr);
                            labelCount++;
                        } else if ("rdfs:label".equals(property)) {
//                            final YagoEntity yagoEntity = yagoEntityRepo.findOneByNn(subjectHref);
//                            final YagoLabel yagoLabel = new YagoLabel();
//                            yagoLabel.setEntity(yagoEntity);
//                            yagoLabel.setInLanguage(literalLanguage);
//                            yagoLabel.setValue(literalStr);
//                            yagoLabelRepo.save(yagoLabel);
                            yagoLabelRepo.addLabel(subjectHref, literalLanguage, literalStr);
                            labelCount++;
                        }

                        if (labelCount % 10000 == 0) {
                            log.info("Added {} labels so far...", labelCount);
                        }
                    }
                }
            }

            log.info("Added labels {} ({} KiB)", labelsFile, NUMBER.format(labelsFile.length() / 1024));
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Usage: ./importyagotaxonomy2 YAGO3_FOLDER");
        final String yagoTsvFolder = args[0];

        final TransactionTemplate txTemplate = new TransactionTemplate(txMgr);

        final Set<String> typeHrefs = getSubjectHrefsFor(new File(yagoTsvFolder, "yagoTaxonomy.tsv"));
        txTemplate.execute((tx) -> createInitialTypes(typeHrefs));
//        txTemplate.execute((tx) -> { linkSubclasses(new File(yagoTsvFolder, "yagoTaxonomy.tsv")); return null; });
        // only add labels for existing entities, i.e. types only
        txTemplate.execute((tx) -> { addLabelsOnlyForExisting(typeHrefs, new File(yagoTsvFolder, "yagoLabels.tsv")); return null; });

        log.info("Done");
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(ImportYagoTaxonomy2PostgresApp.class)
                .profiles("importYagoTaxonomy2PostgresApp")
                .web(false)
                .run(args);
    }

}

package org.lskk.lumen.persistence;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.lskk.lumen.core.LumenProperty;
import org.lskk.lumen.persistence.neo4j.PartitionKey;
import org.lskk.lumen.persistence.neo4j.ThingRepository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.template.Neo4jTemplate;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;

/**
 * PostgreSQL is used to store labels and glossary, Neo4j is used to store taxonomy hierarchy (e.g. {@link org.apache.jena.vocabulary.RDFS#subClassOf},
 * {@link org.apache.jena.vocabulary.RDFS#subPropertyOf}, {@link org.apache.jena.vocabulary.RDF#type}).
 *
 * This works together with {@link ImportYagoTaxonomy2PostgresApp}.
 * -Xmx4g please
 */
@SpringBootApplication(exclude={
        //CrshAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JmxAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        GroovyTemplateAutoConfiguration.class,
        CamelAutoConfiguration.class},
        scanBasePackageClasses = {ThingRepository.class})
@Profile("importYagoTaxonomy2Neo4jApp")
public class ImportYagoTaxonomy2Neo4jApp implements CommandLineRunner {

    protected static final Logger log = LoggerFactory.getLogger(ImportYagoTaxonomy2Neo4jApp.class);
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.US);
    public static final int BUFFER_SIZE = 16 * 1024 * 1024;

    @Inject
    private Environment env;
    @Inject
    private Session session;
    private Neo4jTemplate neo4j;

    /**
     * Merge {@link org.lskk.lumen.persistence.neo4j.SemanticProperty} and meta-types from {@code yagoSchema.tsv}.
     * @param schemaFile
     * @throws IOException
     * @throws InterruptedException
     */
    public void mergeSchema(final File schemaFile) throws IOException, InterruptedException {
        log.info("Ensuring properties and meta-types {} ({} KiB) ...", schemaFile, NUMBER.format(schemaFile.length() / 1024));

        final File typesForNeo4jFile = new File("work/yago_schema_type_for_neo4j.csv");
        final File domainsForNeo4jFile = new File("work/yago_schema_domain_for_neo4j.csv");
        final File rangesForNeo4jFile = new File("work/yago_schema_range_for_neo4j.csv");
        final File subPropertiesForNeo4jFile = new File("work/yago_schema_subpropertyof_for_neo4j.csv");
        typesForNeo4jFile.getParentFile().mkdirs();
        if (!typesForNeo4jFile.exists() || !domainsForNeo4jFile.exists() || !rangesForNeo4jFile.exists() || !subPropertiesForNeo4jFile.exists()) {
            log.info("Creating Neo4j work CSV files {}, {}, {}, {}, from YAGO types {} ({} KiB) ...",
                    typesForNeo4jFile, domainsForNeo4jFile, rangesForNeo4jFile, subPropertiesForNeo4jFile,
                    schemaFile, NUMBER.format(schemaFile.length() / 1024));

            final List<String[]> propToTypes = new ArrayList<>();
            final List<String[]> propToDomains = new ArrayList<>();
            final List<String[]> propToRanges = new ArrayList<>();
            final List<String[]> subProperties = new ArrayList<>();

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(schemaFile), StandardCharsets.UTF_8), BUFFER_SIZE);
                 final CSVReader csv = new CSVReader(reader, '\t', CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {

                while (true) {
                    final String[] line = csv.readNext();
                    if (line == null) {
                        break;
                    }
                    if (line[1].startsWith("<yagoTheme_")) {
                        continue;
                    }

                    String property = line[2];
                    if (property.startsWith("<")) {
                        property = "yago:" + property.replaceAll("[<>]", "");
                    }
                    // most of yagoSchema.tsv is imported, containing: yago:hasConfidence (skipped), rdf:type, rdfs:domain, rdfs:range, rdfs:subPropertyOf
//                        if (!"rdfs:subClassOf".equals(property)) {
//                            continue;
//                        }

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

                    if (LumenProperty.rdf_type.getQName().equals(property)) {
                        propToTypes.add(new String[] { subjectHref, objectHref});
                    } else if (LumenProperty.rdfs_domain.getQName().equals(property)) {
                        propToDomains.add(new String[] { subjectHref, objectHref });
                    } else if (LumenProperty.rdfs_range.getQName().equals(property)) {
                        propToRanges.add(new String[] { subjectHref, objectHref });
                    } else if (LumenProperty.rdfs_subPropertyOf.getQName().equals(property)) {
                        subProperties.add(new String[] { subjectHref, objectHref });
                    }
                }

            }

            log.info("Writing {} rdf:type to {} ...", propToTypes.size(), typesForNeo4jFile);
            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(typesForNeo4jFile), StandardCharsets.UTF_8));
                final CSVWriter csv = new CSVWriter(writer)) {
                csv.writeNext(new String[]{"property", "rdf_type"});
                propToTypes.forEach(csv::writeNext);
            }

            log.info("Writing {} rdfs:domain to {} ...", propToDomains.size(), typesForNeo4jFile);
            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(domainsForNeo4jFile), StandardCharsets.UTF_8));
                final CSVWriter csv = new CSVWriter(writer)) {
                csv.writeNext(new String[]{"property", "rdfs_domain"});
                propToDomains.forEach(csv::writeNext);
            }

            log.info("Writing {} rdfs:range to {} ...", propToRanges.size(), typesForNeo4jFile);
            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(rangesForNeo4jFile), StandardCharsets.UTF_8));
                final CSVWriter csv = new CSVWriter(writer)) {
                csv.writeNext(new String[]{"property", "rdfs_range"});
                propToDomains.forEach(csv::writeNext);
            }

            log.info("Writing {} rdf:subPropertyOf to {} ...", subProperties.size(), subPropertiesForNeo4jFile);
            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(subPropertiesForNeo4jFile), StandardCharsets.UTF_8));
                final CSVWriter csv = new CSVWriter(writer)) {
                csv.writeNext(new String[]{"subProperty", "superProperty"});
                subProperties.forEach(csv::writeNext);
            }
        } else {
            log.info("Reusing existing files {}, {}, {}, {}",
                    typesForNeo4jFile, domainsForNeo4jFile, rangesForNeo4jFile, subPropertiesForNeo4jFile);
        }

        final String typeCypher = "USING PERIODIC COMMIT\n" +
                "LOAD CSV WITH HEADERS FROM '" + typesForNeo4jFile.toURI() + "' AS line\n" +
                "MERGE (s:rdf_Property {nn: line.property, _partition: {partitionKey}})\n" +
                "MERGE (o:owl_Thing {nn: line.rdf_type, _partition: {partitionKey}})\n" +
                "MERGE (s) -[:rdf_type]-> (o)";
        log.info("Executing: {} ...", typeCypher);
        session.query(typeCypher, ImmutableMap.of("partitionKey", PartitionKey.lumen_yago.name()));

        final String domainCypher = "USING PERIODIC COMMIT\n" +
                "LOAD CSV WITH HEADERS FROM '" + domainsForNeo4jFile.toURI() + "' AS line\n" +
                "MERGE (s:rdf_Property {nn: line.property, _partition: {partitionKey}})\n" +
                "MERGE (o:owl_Thing {nn: line.rdfs_domain, _partition: {partitionKey}})\n" +
                "MERGE (s) -[:rdfs_domain]-> (o)";
        log.info("Executing: {} ...", domainCypher);
        session.query(domainCypher, ImmutableMap.of("partitionKey", PartitionKey.lumen_yago.name()));

        final String rangeCypher = "USING PERIODIC COMMIT\n" +
                "LOAD CSV WITH HEADERS FROM '" + rangesForNeo4jFile.toURI() + "' AS line\n" +
                "MERGE (s:rdf_Property {nn: line.property, _partition: {partitionKey}})\n" +
                "MERGE (o:owl_Thing {nn: line.rdfs_range, _partition: {partitionKey}})\n" +
                "MERGE (s) -[:rdfs_range]-> (o)";
        log.info("Executing: {} ...", rangeCypher);
        session.query(rangeCypher, ImmutableMap.of("partitionKey", PartitionKey.lumen_yago.name()));

        final String subPropertyOfCypher = "USING PERIODIC COMMIT\n" +
                "LOAD CSV WITH HEADERS FROM '" + subPropertiesForNeo4jFile.toURI() + "' AS line\n" +
                "MERGE (s:rdf_Property {nn: line.subProperty, _partition: {partitionKey}})\n" +
                "MERGE (o:rdf_Property {nn: line.superProperty, _partition: {partitionKey}})\n" +
                "MERGE (s) -[:rdfs_subPropertyOf]-> (o)";
        log.info("Executing: {} ...", subPropertyOfCypher);
        session.query(subPropertyOfCypher, ImmutableMap.of("partitionKey", PartitionKey.lumen_yago.name()));
    }

    /**
     * Create initial taxonomy type nodes from taxonomyFile.
     * @param taxonomyFile
     * @throws IOException
     * @throws InterruptedException
     */
    public void mergeTypes(final File taxonomyFile) throws IOException, InterruptedException {
        log.info("Merge types {} ({} KiB) ...", taxonomyFile, NUMBER.format(taxonomyFile.length() / 1024));

        final File forNeo4jFile = new File("work/yago_types_for_neo4j.csv");
        forNeo4jFile.getParentFile().mkdirs();
        if (!forNeo4jFile.exists()) {
            log.info("Creating Neo4j work CSV file {} from YAGO types {} ({} KiB) ...", forNeo4jFile, taxonomyFile,
                    NUMBER.format(taxonomyFile.length() / 1024));
            final Set<String> typeHrefs = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(taxonomyFile), StandardCharsets.UTF_8), BUFFER_SIZE)) {
                try (final CSVReader csv = new CSVReader(reader, '\t', CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {

                    while (true) {
                        final String[] line = csv.readNext();
                        if (line == null) {
                            break;
                        }

                        final String property = line[2];
                        // no-op in this case, yagoTaxonomy.tsv *only* contains rdfs:subClassOf statements
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

            log.info("Writing {} types to {} ...", typeHrefs.size(), forNeo4jFile);
            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(forNeo4jFile), StandardCharsets.UTF_8))) {
                try (final CSVWriter csv = new CSVWriter(writer)) {
                    csv.writeNext(new String[]{"nn"});
                    for (final String typeHref : typeHrefs) {
                        csv.writeNext(new String[]{typeHref});
                    }
                }
            }
        } else {
            log.info("Reusing existing file {}", forNeo4jFile);
        }

        final String cypher = "USING PERIODIC COMMIT\n" +
                "LOAD CSV WITH HEADERS FROM '" + forNeo4jFile.toURI() + "' AS line\n" +
                "MERGE (:owl_Thing {nn: line.nn, _partition: {partitionKey}})";
        log.info("Executing: {} ...", cypher);
        session.query(cypher, ImmutableMap.of("partitionKey", PartitionKey.lumen_yago.name()));
    }

    protected void purgeYagoPartition() {
        log.info("Purging lumen_yago...");
        // Yago has almost 21 million nodes, so at 100,000 a batch we need 210 iterations
        while (true) {
            final int nodesDeleted = session.doInTransaction((requestHandler, transaction, metaData) -> {
                final Result result = session.query("MATCH (n:owl_Thing {_partition: {partitionKey}}) WITH n LIMIT 100000 DETACH DELETE n",
                        ImmutableMap.of("partitionKey", PartitionKey.lumen_yago.name()));
                return result.queryStatistics().getNodesDeleted();
            });
            log.info("Deleted {} owl_Thing nodes in lumen_yago ...", nodesDeleted);
            if (nodesDeleted <= 0) {
                break;
            }
        }
        log.info("Purged lumen_yago.");
    }

    /**
     * Create initial taxonomy type nodes from taxonomyFile.
     * @param typesFile yagoTypes.tsv
     * @throws IOException
     * @throws InterruptedException
     */
    public void createEntities(final File typesFile) throws IOException, InterruptedException {
        log.info("Ensuring YAGO entities {} ({} KiB) ...", typesFile, NUMBER.format(typesFile.length() / 1024));

        final Set<String> thingHrefs = new HashSet<>();

        final File forNeo4jFile = new File("work/yago_things_for_neo4j.csv");
        forNeo4jFile.getParentFile().mkdirs();
        if (!forNeo4jFile.exists()) {
            log.info("Creating Neo4j work CSV file {} from YAGO entities {} ({} KiB) ...", forNeo4jFile, typesFile, NUMBER.format(typesFile.length() / 1024));
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(typesFile), StandardCharsets.UTF_8), BUFFER_SIZE)) {
                try (final CSVReader csv = new CSVReader(reader, '\t', CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {

                    while (true) {
                        final String[] line = csv.readNext();
                        if (line == null) {
                            break;
                        }

                        final String property = line[2];
                        // no-op in this case, yagoTypes.tsv *only* contains rdf:type statements
                        if (!"rdf:type".equals(property)) {
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

                        thingHrefs.add(subjectHref);
                        thingHrefs.add(objectHref);
                    }

                }
            }

            log.info("Writing {} nodeNames to {} ...", thingHrefs.size(), forNeo4jFile);
            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(forNeo4jFile), StandardCharsets.UTF_8))) {
                try (final CSVWriter csv = new CSVWriter(writer)) {
                    csv.writeNext(new String[]{"nn"});
                    for (final String thingHref : thingHrefs) {
                        csv.writeNext(new String[]{thingHref});
                    }
                }
            }
        } else {
            log.info("Reusing existing file {}", forNeo4jFile);
        }

        final String cypher = "USING PERIODIC COMMIT\n" +
                "LOAD CSV WITH HEADERS FROM '" + forNeo4jFile.toURI() + "' AS line\n" +
                "CREATE (:owl_Thing {nn: line.nn, _partition: {partitionKey}})";
        log.info("Executing: {} ...", cypher);
        session.query(cypher, ImmutableMap.of("partitionKey", PartitionKey.lumen_yago.name()));
    }

    /**
     * Link subclasses from taxonomyFile.
     * @param taxonomyFile
     * @throws IOException
     * @throws InterruptedException
     */
    public void linkSubclasses(final File taxonomyFile) throws IOException, InterruptedException {
        log.info("Link subclasses {} ({} KiB) ...", taxonomyFile, NUMBER.format(taxonomyFile.length() / 1024));

        final File forNeo4jFile = new File("work/yago_subclasses_for_neo4j.csv");
        forNeo4jFile.getParentFile().mkdirs();
        if (!forNeo4jFile.exists()) {
            log.info("Creating Neo4j work CSV file {} from YAGO taxonomy {} ({} KiB) ...", forNeo4jFile, taxonomyFile,
                    NUMBER.format(taxonomyFile.length() / 1024));

            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(forNeo4jFile), StandardCharsets.UTF_8))) {
                try (final CSVWriter csvWriter = new CSVWriter(writer)) {
                    csvWriter.writeNext(new String[]{"subClass", "superClass"});

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(taxonomyFile), StandardCharsets.UTF_8), BUFFER_SIZE)) {
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

                                csvWriter.writeNext(new String[] { subjectHref, objectHref });
                            }
                        }
                    }
                }
            }

        } else {
            log.info("Reusing existing file {}", forNeo4jFile);
        }

        final String cypher = "USING PERIODIC COMMIT\n" +
                "LOAD CSV WITH HEADERS FROM '" + forNeo4jFile.toURI() + "' AS line\n" +
                "MATCH (subClass:owl_Thing {nn: line.subClass, _partition: {partitionKey}}), (superClass:owl_Thing {nn: line.superClass, _partition: {partitionKey}})\n" +
                "CREATE (subClass) -[:rdfs_subClassOf]-> (superClass)";
        log.info("Executing: {} ...", cypher);
        session.query(cypher, ImmutableMap.of("partitionKey", PartitionKey.lumen_yago.name()));
    }

    /**
     * Link {@link org.apache.jena.vocabulary.RDF#type} from {@code yagoTypes.tsv}.
     * @param typesFile
     * @throws IOException
     * @throws InterruptedException
     */
    public void linkInstanceOf(final File typesFile) throws IOException, InterruptedException {
        log.info("Link rdf:type {} ({} KiB) ...", typesFile, NUMBER.format(typesFile.length() / 1024));

        final File forNeo4jFile = new File("work/yago_rdftype_for_neo4j.csv");
        forNeo4jFile.getParentFile().mkdirs();
        if (!forNeo4jFile.exists()) {
            log.info("Creating Neo4j work CSV file {} from YAGO types {} ({} KiB) ...", forNeo4jFile, typesFile,
                    NUMBER.format(typesFile.length() / 1024));

            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(forNeo4jFile), StandardCharsets.UTF_8))) {
                try (final CSVWriter csvWriter = new CSVWriter(writer)) {
                    csvWriter.writeNext(new String[]{"instance", "type"});

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(typesFile), StandardCharsets.UTF_8), BUFFER_SIZE)) {
                        try (final CSVReader csv = new CSVReader(reader, '\t', CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {

                            while (true) {
                                final String[] line = csv.readNext();
                                if (line == null) {
                                    break;
                                }

                                final String property = line[2];
                                if (!"rdf:type".equals(property)) {
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

                                csvWriter.writeNext(new String[] { subjectHref, objectHref });
                            }
                        }
                    }
                }
            }

        } else {
            log.info("Reusing existing file {}", forNeo4jFile);
        }

        final String cypher = "USING PERIODIC COMMIT\n" +
                "LOAD CSV WITH HEADERS FROM '" + forNeo4jFile.toURI() + "' AS line\n" +
                "MATCH (instanceThing:owl_Thing {nn: line.instance, _partition: {partitionKey}}), (typeThing:owl_Thing {nn: line.type, _partition: {partitionKey}})\n" +
                "CREATE (instanceThing) -[:rdf_type]-> (typeThing)";
        log.info("Executing: {} ...", cypher);
        session.query(cypher, ImmutableMap.of("partitionKey", PartitionKey.lumen_yago.name()));
    }

//    /**
//     * Add labels from labelsFile.
//     * @param labelsFile
//     * @throws IOException
//     * @throws InterruptedException
//     */
//    public void addLabels(final Set<String> typeHrefs, final File labelsFile) throws IOException, InterruptedException {
//        log.info("Add labels {} ({} KiB) ...", labelsFile, NUMBER.format(labelsFile.length() / 1024));
//
//        try (final Transaction tx = taxonomyDb().beginTx()) {
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(labelsFile), StandardCharsets.UTF_8), 1024 * 1024)) {
//                try (final CSVReader csv = new CSVReader(reader, '\t', CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {
//                    while (true) {
//                        final String[] line = csv.readNext();
//                        if (line == null) {
//                            break;
//                        }
//
//                        final String subject = line[1];
//                        final String subjectHref;
//                        if (subject.startsWith("<")) {
//                            subjectHref = "yago:" + subject.replaceAll("[<>]", "");
//                        } else {
//                            subjectHref = subject;
//                        }
//
//                        if (!typeHrefs.contains(subjectHref)) {
//                            continue;
//                        }
//
//                        final String property = line[2];
//                        if (ImmutableSet.of("skos:prefLabel", "<isPreferredMeaningOf>").contains(property)) {
//                            final String resOrLiteral = line[3];
//
//                            final Node_Literal literal = (Node_Literal) NodeFactoryExtra.parseNode(resOrLiteral, RdfUtils.getPREFIX_MAP());
//                            final String literalStr = literal.getLiteralValue().toString();
//
//                            final ImmutableMap<String, Object> props = ImmutableMap.of("href", subjectHref, "literalStr", literalStr);
//                            if ("skos:prefLabel".equals(property)) {
//                                try (Result res = taxonomyDb().execute("MATCH (n:schema_Thing {nn: {href}}) SET n.prefLabel = {literalStr}", props)) { }
//                            } else if ("<isPreferredMeaningOf>".equals(property)) {
//                                try (Result res = taxonomyDb().execute("MATCH (n:schema_Thing {nn: {href}}) SET n.isPreferredMeaningOf = {literalStr}", props)) { }
//                            }
//                        }
//                    }
//                }
//            }
//
//            log.info("Committing transaction...");
//            tx.success();
//        }
//    }

    @Override
    public void run(String... args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Usage: ./importyagotaxonomy YAGO3_FOLDER");
        final File yagoWorkFolder = new File(args[0]);
        neo4j = new Neo4jTemplate(session);

        // indexes are created by Neo4jConfig.IndexesConfig
//        try (final Transaction tx = taxonomyDb().beginTx()) {
//
//            log.info("Ensuring constraints and indexes");
//            try (Result res = taxonomyDb().execute("CREATE INDEX ON :owl_Thing(_partition)")) {}
//            try (Result res = taxonomyDb().execute("CREATE INDEX ON :owl_Thing(nn)")) {}
//            try (Result res = taxonomyDb().execute("CREATE INDEX ON :owl_Thing(prefLabel)")) {}
//            try (Result res = taxonomyDb().execute("CREATE INDEX ON :owl_Thing(isPreferredMeaningOf)")) {
//            }
//
//            log.info("Committing transaction...");
//            tx.success();
//        }

//        purgeYagoPartition();
        mergeSchema(new File(yagoWorkFolder, "yagoSchema.tsv"));
//        mergeTypes(new File(yagoWorkFolder, "yagoTaxonomy.tsv"));
//        createEntities(new File(yagoWorkFolder, "yagoTypes.tsv"));
//        linkSubclasses(new File(yagoWorkFolder, "yagoTaxonomy.tsv"));
//        linkInstanceOf(new File(yagoWorkFolder, "yagoTypes.tsv"));
//        addLabels(typeHrefs, new File(args[0], "yagoLabels.tsv"));

        log.info("Done");
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(ImportYagoTaxonomy2Neo4jApp.class)
                .profiles("importYagoTaxonomy2Neo4jApp", "spring-data-neo4j")
                .web(false)
                .run(args);
    }

}

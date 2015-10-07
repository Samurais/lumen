package org.lskk.lumen.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;

/**
 * Import labeled resources (only) from {@code yagoLabels.jsonset}
 * Note: It uses BatchInserter, perhaps at least -Xmx4g is needed, unless you also use tmpfs
 */
@SpringBootApplication
@Profile("importlabels")
public class LumenPersistenceImportLabelsApp implements CommandLineRunner {
    @PostConstruct
    public void init() {
        mapper = new ObjectMapper();
    }

    public void importLabels(File dbPath, final File file) throws IOException {
        log.info("Importing {} ({} KiB) to {} ...", file, NUMBER.format(DefaultGroovyMethods.asType(file.length() / 1024, Long.class)), dbPath);
        long importeds = 0l;

        final BatchInserter inserter = BatchInserters.inserter(dbPath.getPath());
        try {
            final Label resourceLabel = DynamicLabel.label("Resource");
            final Label labelLabel = DynamicLabel.label("Label");
            inserter.createDeferredConstraint(resourceLabel).assertPropertyIsUnique("href").create();

            // PLEASE create the label indexes on last step, not now! to speed up imports on later stages
//            inserter.createDeferredSchemaIndex(resourceLabel).on('prefLabel').create()
//            inserter.createDeferredSchemaIndex(resourceLabel).on('isPreferredMeaningOf').create()
//            inserter.createDeferredSchemaIndex(labelLabel).on('v').create()

            final DynamicRelationshipType rdfsLabel = DynamicRelationshipType.withName("rdfs_label");

            try (final Scanner scanner = new Scanner(file, StandardCharsets.UTF_8.name())) {
                scanner.useDelimiter("\u0001");// Ctrl+A (SOH)
                while (true) {
                    final Object line;
                    try {
                        line = scanner.next();
                    } catch (NoSuchElementException ex) {
                        break;
                    }

                    final IndexedResource res = mapper.readValue((String) line, IndexedResource.class);
                    if (res.getPrefLabel() == null && res.getLabels() != null && !res.getLabels().isEmpty()) {
                        res.setPrefLabel(res.getLabels().get(0).getValue());
                        res.getLabels().remove(0);
                    }

                    final Map<String, Object> props = new HashMap<>();
                    if (res.getPrefLabel() != null) {
                        props.put("prefLabel", res.getPrefLabel());
                    }

                    if (res.getIsPreferredMeaningOf() != null) {
                        props.put("isPreferredMeaningOf", res.getIsPreferredMeaningOf());
                    }

                    final long resNode = inserter.createNode(props, resourceLabel);
                    for (IndexedResource.LocalizedLabel it : res.getLabels()) {
                        final Map<String, Object> labelProps = new HashMap<>();

                        if (it.getLanguage() != null) {
                            labelProps.put("l", it.getLanguage());
                        }

                        final long labelNode = inserter.createNode(labelProps, labelLabel);
                        inserter.createRelationship(resNode, labelNode, rdfsLabel, new LinkedHashMap());
                    }
                    importeds++;

                    if (importeds % 10000 == 0) {
                        log.info("Imported {} resource nodes +prefLabel +isPreferredMeaningOf +label relationships from {}", NUMBER.format(importeds), file);
                    }

                }

            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot process", e);
        } finally {
            log.info("Shutting down batchInserter after importing {} resource nodes from {} ...", NUMBER.format(importeds), file);
            inserter.shutdown();
        }

        log.info("Completed importing {} resource nodes +prefLabel +isPreferredMeaningOf +label relationships from {}", NUMBER.format(importeds), file);
    }

    @Override
    public void run(String... args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "yago2s-directory argument is required");

//        final file = new File(args[0], 'yagoSimpleTaxonomy.tsv')
        // step 2: Import Labels (426 MiB: 2,955,210 resources)
        importLabels(env.getRequiredProperty("neo4j.path", File.class), new File(args[0], "yagoLabels.jsonset"));
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(LumenPersistenceImportLabelsApp.class).profiles("importlabels", "batchinserter").run(args);
    }

    public static String getLUMEN_NAMESPACE() {
        return LUMEN_NAMESPACE;
    }

    protected static final Logger log = LoggerFactory.getLogger(LumenPersistenceImportLabelsApp.class);
    private static final String LUMEN_NAMESPACE = "http://lumen.lskk.ee.itb.ac.id/resource/";
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.US);
    @Inject
    protected Environment env;
    protected ObjectMapper mapper;
}

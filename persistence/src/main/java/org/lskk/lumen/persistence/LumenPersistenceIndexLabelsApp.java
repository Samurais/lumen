package org.lskk.lumen.persistence;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.sparql.util.NodeFactoryExtra;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Note: It loads and indexes 1.1 GB of labels data to RAM!, GC limit exceeded may happen.
 * At least -Xmx4g is recommended
 * <p>
 * There are 3 label properties:
 * 1. skos:prefLabel -- mandatory for Lumen, only stored in node, language info lost, fact ID lost
 * 2. rdfs:label -- 0 or more, stored as Label(v,l) nodes, with 'rdfs_label' relationship (optionally with 'f' fact ID). skipped if same as skos:prefLabel
 * 3. yago:isPreferredMeaningOf -- optional, only stored in node, language info lost, fact ID lost
 * <p>
 * These three properties are treated specially by Lumen and will not be imported "normally" to Neo4j.
 */
@SpringBootApplication
@Profile("indexlabels")
public class LumenPersistenceIndexLabelsApp implements CommandLineRunner {
    protected static final Logger log = LoggerFactory.getLogger(LumenPersistenceIndexLabelsApp.class);
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.US);
    protected ObjectMapper mapper;

    @PostConstruct
    public void init() {
        mapper = new ObjectMapper();
    }

    public void indexLabels(final File file, File outFile) throws IOException {
        final Map<String, IndexedResource> resources = new LinkedHashMap<String, IndexedResource>();

        log.info("Indexing node labels of {} ({} KiB) to {} ...", file, NUMBER.format(DefaultGroovyMethods.asType(file.length() / 1024, Long.class)), outFile);
        long importeds = 0l;
        long readCount = 0l;
        try (final BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), (int) 1024 * 1024)) {
            try (final CSVReader csv = new CSVReader(buf, StringGroovyMethods.asType("\t", Character.class), CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)) {
                String[] line = csv.readNext();
                while (line != null) {
                    final String factId = Strings.emptyToNull(line[0]);
                    final String factHref = factId != null ? "yago:" + factId.replaceAll("[<>]", "") : null;
                    final String factCypher = factHref != null ? "f: {factHref}" : "";
                    final String subject = line[1];
                    final String subjectHref = "yago:" + subject.replaceAll("[<>]", "");
                    final String property = line[2];
                    final Object relName;
                    if (property.startsWith("<")) {
                        relName = "yago_" + property.replaceAll("[<>]", "");
                    } else {
                        relName = property.replace(":", "_");
                    }

                    final String resOrLiteral = line[3];
                    if (!resOrLiteral.startsWith("<")) {// Only keep Literal
                        IndexedResource idxRes = resources.get(subjectHref);
                        if (idxRes == null) {
                            idxRes = new IndexedResource();
                            resources.put(subjectHref, idxRes);
                        }


                        final Node_Literal literal = (Node_Literal) NodeFactoryExtra.parseNode(resOrLiteral);
                        final String literalValue = literal.getLiteralValue().toString();

                        final Object merge;
                        if ("skos:prefLabel".equals(property)) {
                            idxRes.setPrefLabel(literalValue);
                            importeds++;
                        } else if ("<isPreferredMeaningOf>".equals(property)) {
                            idxRes.setIsPreferredMeaningOf(literalValue);
                            importeds++;
                        } else if ("rdfs:label".equals(property)) {
                            idxRes.addLabel(literalValue, Strings.emptyToNull(literal.getLiteralLanguage()));
                            importeds++;
                        } else {
                            // ignored
                        }

                    }

                    readCount++;
                    if (readCount % 100000 == 0) {
                        log.info("Indexed {} resources: {} out of {} statements from {}", 
                                NUMBER.format(resources.size()), NUMBER.format(importeds), NUMBER.format(readCount), file);
                    }

                    line = csv.readNext();
                }
            }
        }
        log.info("Completed indexing {} resources: {} out of {} statements from {}, writing out to {} ...", 
                NUMBER.format(resources.size()), NUMBER.format(importeds), NUMBER.format(readCount), file, outFile);
        
        try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8), (int) 1024 * 1024)) {
            for (Map.Entry<String, IndexedResource> entry : resources.entrySet()) {
                final String href = entry.getKey();
                final IndexedResource res = entry.getValue();
                res.setHref((String) href);

                // normalize prefLabel if possible
                if (res.getPrefLabel() == null && res.getLabels() != null && !res.getLabels().isEmpty()) {
                    res.setPrefLabel(res.getLabels().get(0).getValue());
                    res.getLabels().remove(0);
                }

                if (res.getPrefLabel() != null && res.getLabels() != null) {
                    res.getLabels().remove(res.getPrefLabel());
                }

                writer.write(mapper.writeValueAsString(res));
                writer.write("\u0001");// Ctrl+A (SOH)
            }
        }
        log.info("Saved {} resources to {}", NUMBER.format(resources.size()), outFile);
    }

    @Override
    public void run(String... args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "yago2s-directory argument is required");
        // step 1: Labels (1.1 GB: 15.372.313 lines)
        indexLabels(new File(args[0], "yagoLabels.tsv"), new File(args[0], "yagoLabels.jsonset"));
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(LumenPersistenceIndexLabelsApp.class).profiles("indexlabels", "batchinserter").run(args);
    }

}

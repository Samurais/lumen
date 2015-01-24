package id.ac.itb.lumen.persistence

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import com.google.common.base.Strings
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import com.hp.hpl.jena.graph.Node_Literal
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra
import groovy.transform.CompileStatic
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.GraphDatabaseService
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
 * Note: It loads and indexes 1.1 GB of labels data to RAM!, GC limit exceeded may happen.
 * At least -Xmx4g is recommended
 *
 * There are 3 label properties:
 * 1. skos:prefLabel -- mandatory for Lumen, only stored in node, language info lost, fact ID lost
 * 2. rdfs:label -- 0 or more, stored as Label(v,l) nodes, with 'rdfs_label' relationship (optionally with 'f' fact ID). skipped if same as skos:prefLabel
 * 3. yago:isPreferredMeaningOf -- optional, only stored in node, language info lost, fact ID lost
 *
 * These three properties are treated specially by Lumen and will not be imported "normally" to Neo4j.
 */
@CompileStatic
@SpringBootApplication
@Profile('indexlabels')
class LumenPersistenceIndexLabelsApp implements CommandLineRunner {

    protected static final Logger log = LoggerFactory.getLogger(LumenPersistenceIndexLabelsApp)
    static final LUMEN_NAMESPACE = 'http://lumen.lskk.ee.itb.ac.id/resource/'
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.ENGLISH)
    
    protected ObjectMapper mapper

    @PostConstruct
    void init() {
        mapper = new ObjectMapper()
    }

    void indexLabels(File file, File outFile) {
        Map<String, IndexedResource> resources = [:]

        log.info('Indexing node labels of {} ({} KiB) to {} ...', file, NUMBER.format((file.length() / 1024) as long), outFile)
        long importeds = 0
        long readCount = 0
        new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))
                .withReader { Reader buf ->
            final csv = new CSVReader(buf, '\t' as char, CSVParser.NULL_CHARACTER, CSVParser.NULL_CHARACTER)
            csv.withCloseable {
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
                    if (!resOrLiteral.startsWith('<')) { // Only keep Literal
                        def idxRes = resources.get(subjectHref)
                        if (idxRes == null) {
                            idxRes = new IndexedResource()
                            resources.put(subjectHref, idxRes)
                        }

                        final literal = (Node_Literal) NodeFactoryExtra.parseNode(resOrLiteral)
                        final literalValue = literal.literalValue.toString()

                        final merge
                        if ('skos:prefLabel'.equals(property)) {
                            idxRes.prefLabel = literalValue
                            importeds++
                        } else if ('yago:isPreferredMeaningOf'.equals(property)) {
                            idxRes.isPreferredMeaningOf = literalValue
                            importeds++
                        } else if ('rdfs:label'.equals(property)) {
                            idxRes.addLabel(literalValue, Strings.emptyToNull(literal.literalLanguage))
                            importeds++
                        } else {
                            // ignored
                        }
                    }

                    readCount++
                    if (readCount % 100000 == 0) {
                        log.info('Indexed {} resources: {} out of {} statements from {}',
                                NUMBER.format(resources.size()), NUMBER.format(importeds), NUMBER.format(readCount), file)
                    }

                    line = csv.readNext()
                }
            }
        }
        log.info('Completed indexing {} resources: {} out of {} statements from {}, writing out to {} ...',
                NUMBER.format(resources.size()), NUMBER.format(importeds), NUMBER.format(readCount), file, outFile)

        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))
                .withWriter { Writer writer ->
            resources.each { href, res ->
                res.href = href

                // normalize prefLabel if possible
                if (res.prefLabel == null && res.labels != null && !res.labels.isEmpty()) {
                    res.prefLabel = res.labels[0].value
                    res.labels.remove(0)
                }
                if (res.prefLabel != null && res.labels != null) {
                    res.labels.remove(res.prefLabel)
                }

                writer.write(mapper.writeValueAsString(res))
                writer.write('\u0001') // Ctrl+A (SOH)
            }
        }
        log.info('Saved {} resources to {}',
                NUMBER.format(resources.size()), outFile)
    }
    
    @Override
    void run(String... args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, 'yago2s-directory argument is required' as Object)
        // step 1: Labels (1.1 GB: 15.372.313 lines)
        indexLabels(new File(args[0], 'yagoLabels.tsv'), new File(args[0], 'yagoLabels.jsonset'))
    }

    static void main(String[] args) {
        new SpringApplicationBuilder(LumenPersistenceIndexLabelsApp)
                .profiles('indexlabels', 'batchinserter')
                .run(args)
    }
}

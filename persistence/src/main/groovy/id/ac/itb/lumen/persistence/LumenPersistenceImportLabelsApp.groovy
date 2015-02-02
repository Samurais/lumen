package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Preconditions
import groovy.transform.CompileStatic
import org.neo4j.graphdb.DynamicLabel
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.unsafe.batchinsert.BatchInserters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.nio.charset.StandardCharsets
import java.text.NumberFormat

/**
 * Import labeled resources (only) from {@code yagoLabels.jsonset}
 * Note: It uses BatchInserter, perhaps at least -Xmx4g is needed, unless you also use tmpfs
 */
@CompileStatic
@SpringBootApplication
@Profile('importlabels')
class LumenPersistenceImportLabelsApp implements CommandLineRunner {

    protected static final Logger log = LoggerFactory.getLogger(LumenPersistenceImportLabelsApp)
    static final LUMEN_NAMESPACE = 'http://lumen.lskk.ee.itb.ac.id/resource/'
    protected static final NumberFormat NUMBER = NumberFormat.getNumberInstance(Locale.ENGLISH)

    @Inject
    protected Environment env

    protected ObjectMapper mapper
    
    @PostConstruct
    void init() {
        mapper = new ObjectMapper()
    }

    void importLabels(File dbPath, File file) {
        log.info('Importing {} ({} KiB) to {} ...', file, NUMBER.format((file.length() / 1024) as long), dbPath)
        long importeds = 0

        final inserter = BatchInserters.inserter(dbPath as String)
        try {
            final resourceLabel = DynamicLabel.label('Resource')
            final labelLabel = DynamicLabel.label('Label')
            inserter.createDeferredConstraint(resourceLabel).assertPropertyIsUnique('href').create()

            // PLEASE create the label indexes on last step, not now! to speed up imports on later stages
//            inserter.createDeferredSchemaIndex(resourceLabel).on('prefLabel').create()
//            inserter.createDeferredSchemaIndex(resourceLabel).on('isPreferredMeaningOf').create()
//            inserter.createDeferredSchemaIndex(labelLabel).on('v').create()

            final rdfsLabel = DynamicRelationshipType.withName('rdfs_label')

            final scanner = new Scanner(file, StandardCharsets.UTF_8 as String)
            scanner.useDelimiter('\u0001') // Ctrl+A (SOH)
            scanner.withCloseable {
                while (true) {
                    final line
                    try {
                        line = scanner.next()
                    } catch (NoSuchElementException ex) {
                        break
                    }
                    final res = mapper.readValue(line, IndexedResource)
                    if (res.prefLabel == null && res.labels != null && !res.labels.isEmpty()) {
                        res.prefLabel = res.labels[0].value
                        res.labels.remove(0)
                    }
                    final props = [href: res.href] as Map<String, Object>
                    if (res.prefLabel != null) {
                        props['prefLabel'] = res.prefLabel
                    }
                    if (res.isPreferredMeaningOf != null) {
                        props['isPreferredMeaningOf'] = res.isPreferredMeaningOf
                    }
                    final resNode = inserter.createNode(props, resourceLabel)
                    res.labels?.each {
                        final labelProps = [v: it.value] as Map<String, Object>
                        if (it.language != null) {
                            labelProps['l'] = it.language
                        }
                        final labelNode = inserter.createNode(labelProps, labelLabel)
                        inserter.createRelationship(resNode, labelNode, rdfsLabel, [:])
                    }
                    importeds++

                    if (importeds % 10000 == 0) {
                        log.info('Imported {} resource nodes +prefLabel +isPreferredMeaningOf +label relationships from {}',
                            NUMBER.format(importeds), file)
                    }
                }
            }
        } finally {
            log.info('Shutting down batchInserter after importing {} resource nodes from {} ...',
                    NUMBER.format(importeds), file)
            inserter.shutdown()
        }
        log.info('Completed importing {} resource nodes +prefLabel +isPreferredMeaningOf +label relationships from {}',
                NUMBER.format(importeds), file)
    }
    
    @Override
    void run(String... args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, 'yago2s-directory argument is required' as Object)

//        final file = new File(args[0], 'yagoSimpleTaxonomy.tsv')
        // step 2: Import Labels (426 MiB: 2,955,210 resources)
        importLabels(env.getRequiredProperty('neo4j.path', File), new File(args[0], 'yagoLabels.jsonset'))
    }

    static void main(String[] args) {
        new SpringApplicationBuilder(LumenPersistenceImportLabelsApp)
                .profiles('importlabels', 'batchinserter')
                .run(args)
    }
}

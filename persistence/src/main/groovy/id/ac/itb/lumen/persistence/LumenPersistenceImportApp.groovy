package id.ac.itb.lumen.persistence

import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Profile
import org.springframework.data.neo4j.support.Neo4jTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

import javax.inject.Inject
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

@CompileStatic
@SpringBootApplication
@EnableTransactionManagement
@Profile('import')
class LumenPersistenceImportApp implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LumenPersistenceImportApp)
    static final LUMEN_NAMESPACE = 'http://lumen.lskk.ee.itb.ac.id/resource/'
    
    @Inject
    protected PersonRepository personRepo
    @Inject
    protected PlatformTransactionManager txMgr
    @Inject
    protected Neo4jTemplate neo4j
    
    @Override
    void run(String... args) throws Exception {
        final txTemplate = new TransactionTemplate(txMgr)

        log.info('Clearing Neo4j database...')
        txTemplate.execute {
            neo4j.query('MATCH (a)-[r]-(b) DELETE r\n' +
                    'MATCH n DELETE n', ImmutableMap.of())
        }
        log.info('Cleared Neo4j database')

        final file = '/media/ceefour/passport/databank/yago2s/yagoSimpleTaxonomy.tsv'
        new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), StandardCharsets.UTF_8)).withReader { buf ->
            new CSVReader(buf, '\t' as char, '"' as char, '\\' as char).with { csv ->
                final line = csv.readNext()
                final factId = line[0]
                final subject = line[1]
                final property = line[2]
                final object = line[3]
                log.debug('{} -[:{}]-> {}    # {}', subject, property, object, factId)
            }
        }
    }

    static void main(String[] args) {
        new SpringApplicationBuilder(LumenPersistenceImportApp)
                .profiles('import')
                .run(args)
    }
}

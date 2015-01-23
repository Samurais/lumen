package id.ac.itb.lumen.persistence

import com.google.common.collect.ImmutableList
import groovy.transform.CompileStatic
import id.ac.itb.lumen.persistence.PersonRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional

import javax.inject.Inject

@CompileStatic
@SpringBootApplication
@EnableTransactionManagement
@Profile('daemon')
class LumenPersistenceApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LumenPersistenceApplication)
    static final LUMEN_NAMESPACE = 'http://lumen.lskk.ee.itb.ac.id/resource/'
    
    @Inject
    protected PersonRepository personRepo
    
    @Override
    @Transactional
    void run(String... args) throws Exception {
        def person = new Person()
        person.label = 'Budhi Yulianto'
        person.uri = LUMEN_NAMESPACE + 'Budhi_Yulianto'
        person = personRepo.save(person)
        final people = ImmutableList.copyOf(personRepo.findAll())
        log.info('People: {}', people)
        Thread.currentThread().join()
    }

    static void main(String[] args) {
        new SpringApplicationBuilder(LumenPersistenceApplication)
                .profiles('daemon')
                .run(args)
    }
}

package id.ac.itb.ee.lskk.lumen.persistence

import id.ac.itb.lumen.persistence.PersonRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

import javax.inject.Inject

@SpringBootApplication
class LumenPersistenceApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LumenPersistenceApplication)
    
    @Inject
    protected PersonRepository personRepo
    
    @Override
    void run(String... args) throws Exception {
        final people = personRepo.findAll()
        log.info('People: {}', people)
    }

    static void main(String[] args) {
        SpringApplication.run LumenPersistenceApplication, args
    }
}

package org.lskk.lumen.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@SpringBootApplication
@Profile("daemon")
public class LumenPersistenceApplication implements CommandLineRunner {
    @Override
    @Transactional
    public void run(String... args) throws Exception {
//        def person = new Person()
//        person.label = 'Budhi Yulianto'
//        person.uri = LUMEN_NAMESPACE + 'Budhi_Yulianto'
//        person = personRepo.save(person)
//        final people = ImmutableList.copyOf(personRepo.findAll())
//        log.info('People: {}', people)
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(LumenPersistenceApplication.class).profiles("daemon", "neo4j-dbs").run(args);
    }

    private static final Logger log = LoggerFactory.getLogger(LumenPersistenceApplication.class);
}

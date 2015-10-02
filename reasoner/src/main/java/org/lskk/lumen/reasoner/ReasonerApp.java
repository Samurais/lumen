package org.lskk.lumen.reasoner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Profile("reasonerApp")
class ReasonerApp implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReasonerApp.class);

    public static void main(String[] args) {
        new SpringApplicationBuilder(ReasonerApp.class)
                .profiles("reasonerApp")
                .run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Joining thread, you can press Ctrl+C to shutdown application");
        Thread.currentThread().join();
    }
}

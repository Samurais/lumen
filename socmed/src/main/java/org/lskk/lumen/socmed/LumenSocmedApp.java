package org.lskk.lumen.socmed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
class LumenSocmedApp implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LumenSocmedApp.class);

    public static void main(String[] args) {
        SpringApplication.run(LumenSocmedApp.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Joining thread, you can press Ctrl+C to shutdown application");
        Thread.currentThread().join();
    }
}

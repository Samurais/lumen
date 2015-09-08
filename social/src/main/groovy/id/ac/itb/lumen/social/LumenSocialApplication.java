package id.ac.itb.lumen.social;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
class LumenSocialApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LumenSocialApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(LumenSocialApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Joining thread, you can press Ctrl+C to shutdown application");
        Thread.currentThread().join();
    }
}

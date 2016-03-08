package org.lskk.lumen.socmed;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.lskk.lumen.core.LumenCoreConfig;
import org.lskk.lumen.core.util.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Profile("socmedApp")
@Import({LumenCoreConfig.class, ProxyConfig.class})
public class LumenSocmedApp implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LumenSocmedApp.class);

    public static void main(String[] args) {
        new SpringApplicationBuilder(LumenSocmedApp.class)
                .profiles("socmedApp", "facebook", "twitter", "rabbitmq", "imgur")
                .run(args);
    }

    @Bean(destroyMethod = "close")
    public CloseableHttpClient httpClient() {
        return HttpClients.createSystem();
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Joining thread, you can press Ctrl+C to shutdown application");
        Thread.currentThread().join();
    }
}

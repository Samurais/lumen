package org.lskk.lumen.socmed;

import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.inject.Inject;

/**
 * Created by ceefour on 1/19/15.
 */
@Configuration
public class RabbitMqConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfig.class);

    @Inject
    private Environment env;

    @Bean
    public ConnectionFactory amqpConnFactory() {
        final ConnectionFactory connFactory = new ConnectionFactory();
        connFactory.setHost(env.getRequiredProperty("amqp.host"));
        connFactory.setUsername(env.getRequiredProperty("amqp.username"));
        connFactory.setPassword(env.getRequiredProperty("amqp.password"));
        log.info("AMQP configuration: host={} username={}", connFactory.getHost(), connFactory.getUsername());
        return connFactory;
    }

}

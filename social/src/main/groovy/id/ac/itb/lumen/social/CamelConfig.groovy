package id.ac.itb.lumen.social

import groovy.transform.CompileStatic
import org.apache.camel.spring.CamelContextFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Configuration
class CamelConfig {

    @Bean
    CamelContextFactoryBean camelContext() {
        final camelContextFactory = new CamelContextFactoryBean()
        camelContextFactory
    }

}

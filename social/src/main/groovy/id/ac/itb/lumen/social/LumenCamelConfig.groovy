package id.ac.itb.lumen.social

import com.google.common.collect.ImmutableList
import groovy.transform.CompileStatic
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.facebook.FacebookEndpoint
import org.apache.camel.spring.javaconfig.CamelConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowire
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component

import javax.inject.Inject
import javax.inject.Named

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Configuration
class LumenCamelConfig extends CamelConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LumenCamelConfig.class)

    @Inject @Named("facebookRouteBuilder")
    private RouteBuilder facebookRouteBuilder

//    @Override
//    List<RouteBuilder> routes() {
//        ImmutableList.of(facebookRouteBuilder)
//    }
}

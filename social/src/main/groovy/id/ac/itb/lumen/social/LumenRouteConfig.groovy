package id.ac.itb.lumen.social

import com.google.common.collect.ImmutableList
import com.sun.javafx.collections.ImmutableObservableList
import groovy.transform.CompileStatic
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.facebook.FacebookEndpoint
import org.apache.camel.spring.javaconfig.CamelConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import javax.inject.Inject

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Configuration
class LumenRouteConfig extends CamelConfiguration {

    @Inject
    AgentRepository agentRepo

    @Bean
    def facebookRouteBuilder(CamelContext camel) {

        new RouteBuilder() {
            @Override
            void configure() throws Exception {
                agentRepo.findAll().each {
                    final facebookHome = camel.getEndpoint('facebook', FacebookEndpoint.class)
                    facebookHome.configuration.setOAuthAppId(it.facebookSys.facebookAppId)
                    facebookHome.configuration.setOAuthAppSecret(it.facebookSys.facebookAppSecret)
                    facebookHome.configuration.setOAuthAccessToken(it.facebookSys.facebookAccessToken)
                    facebookHome.configuration.setName('home')
                    from(facebookHome).to("log:lumen." + it.id)
                }
            }
        }
    }

//    @Override
//    List<RouteBuilder> routes() {
//        ImmutableList.of()
//    }
}

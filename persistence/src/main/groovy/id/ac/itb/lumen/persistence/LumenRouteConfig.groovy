package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.common.collect.FluentIterable
import com.rabbitmq.client.ConnectionFactory
import facebook4j.Post
import groovy.transform.CompileStatic
import id.ac.itb.lumen.core.*
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.facebook.FacebookEndpoint
import org.apache.camel.component.twitter.TwitterEndpoint
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import twitter4j.DirectMessage
import twitter4j.Status

import javax.inject.Inject

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Configuration
class LumenRouteConfig {

    private static final Logger log = LoggerFactory.getLogger(LumenRouteConfig.class)

    @Inject
    protected PersonRepository agentRepo
    @Inject
    protected ToJson toJson

    @Bean
    ConnectionFactory amqpConnFactory() {
      final connFactory = new ConnectionFactory()
      connFactory.host = 'localhost'
      connFactory.username = 'guest'
      connFactory.password = 'guest'
      return connFactory
    }

    //@Bean
    def RouteBuilder personFindAllRouteBuilder() {
        log.info('Initializing personFindAll RouteBuilder')
        new RouteBuilder() {
            @Override
            void configure() throws Exception {
                new RouteBuilder() {
                    @Override
                    void configure() throws Exception {
                        final facebookFeed = "facebook://postStatusMessage?oAuthAppId=${it.facebookSys?.facebookAppId}&oAuthAppSecret=${it.facebookSys?.facebookAppSecret}&oAuthAccessToken=${it.facebookSys?.facebookAccessToken}"
                        final twitterTimelineUser = "twitter://timeline/user?consumerKey=${it.twitterSys?.twitterApiKey}&consumerSecret=${it.twitterSys?.twitterApiSecret}&accessToken=${it.twitterSys?.twitterToken}&accessTokenSecret=${it.twitterSys?.twitterTokenSecret}"
                        from('rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=' + Channel.PERSISTENCE_FACT.key('arkan'))
                                .to('log:persistence-fact')
                                .process {
                            final findAllQuery = toJson.mapper.readValue(it.in.body as byte[], FindAllQuery)
                            switch (statusUpdate.channel.thingId) {
                                case 'facebook':
                                    it.in.headers['network.id'] = 'facebook'
                                    it.in.headers['CamelFacebook.message'] = statusUpdate.message
                                    it.in.body = null
                                    break
                                case 'twitter':
                                    it.in.headers['network.id'] = 'twitter'
                                    it.in.body = statusUpdate.message
                                    break
                            }
                        }.choice()
                                .when(header('network.id').isEqualTo('facebook')).to(facebookFeed).to('log:facebook-postStatusMessage')
                                .when(header('network.id').isEqualTo('twitter')).to(twitterTimelineUser).to('log:twitter-timeline-user')
                                .otherwise().to('log:expression-unknown')
                    }
                }
            }
        }
    }

}

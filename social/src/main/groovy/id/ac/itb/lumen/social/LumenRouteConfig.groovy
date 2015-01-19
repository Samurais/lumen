package id.ac.itb.lumen.social

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.common.collect.ImmutableList
import com.sun.javafx.collections.ImmutableObservableList
import groovy.transform.CompileStatic
import org.apache.camel.CamelContext
import org.apache.camel.Component
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.facebook.FacebookEndpoint
import org.apache.camel.spring.javaconfig.CamelConfiguration
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import javax.inject.Inject

/**
 * Created by ceefour on 1/19/15.
 */
@CompileStatic
@Configuration
class LumenRouteConfig {

    private static final Logger log = LoggerFactory.getLogger(LumenRouteConfig.class)

    @Inject
    protected AgentRepository agentRepo

    @Bean
    def RouteBuilder facebookRouteBuilder() {
        log.info('Initializing facebookRouteBuilder')
        final mapper = new ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
        new RouteBuilder() {
            @Override
            void configure() throws Exception {
//                from("timer:1").to("log:timer")
//                final facebook = getContext().getComponent('facebook')
//                final facebookHome = getContext().getEndpoint('facebook://home', FacebookEndpoint.class)
                final sometimeAgo = new DateTime().minusDays(1)
                agentRepo.findAll().each {
                    final facebookHome = getContext().getEndpoint("facebook://home?consumer.delay=15000&oAuthAppId=${it.facebookSys.facebookAppId}&oAuthAppSecret=${it.facebookSys.facebookAppSecret}&oAuthAccessToken=${it.facebookSys.facebookAccessToken}&reading.since=${sometimeAgo}&reading.limit=20",
                        FacebookEndpoint.class)
/*
Headers: {breadcrumbId=ID-hendy-dev-34504-1421663970968-0-1}
Body: {
  "metadata" : null,
  "id" : "10203536415231264_425825740901928",
  "from" : {
    "id" : "10203536415231264",
    "name" : "Marzuki Syahfirin"
  },
  "to" : null,
  "message" : null,
  "messageTags" : null,
  "picture" : null,
  "link" : null,
  "name" : null,
  "caption" : null,
  "description" : null,
  "source" : null,
  "properties" : null,
  "icon" : null,
  "actions" : [ {
    "name" : "Comment",
    "link" : "https://www.facebook.com/10203536415231264/posts/425825740901928"
  } ],
  "privacy" : {
    "value" : null,
    "friends" : null,
    "networks" : null,
    "allow" : null,
    "deny" : null,
    "description" : null
  },
  "type" : "status",
  "sharesCount" : null,
  "likes" : [ {
    "id" : "10203536415231264",
    "name" : "Marzuki Syahfirin"
  }, {
    "id" : "10206034239078191",
    "name" : "Budhi Yulianto"
  }, {
    "id" : "10203261432890875",
    "name" : "Ary Setijadi Prihatmanto"
  }, {
    "id" : "10152634063888963",
    "name" : "Wahyudi Dan Anasari"
  } ],
  "place" : null,
  "statusType" : null,
  "story" : "Marzuki Syahfirin likes a post.",
  "storyTags" : {
    "0" : [ {
      "metadata" : null,
      "id" : "10203536415231264",
      "name" : "Marzuki Syahfirin",
      "offset" : -1,
      "length" : 17,
      "type" : "user",
      "x" : null,
      "y" : null,
      "createdTime" : null
    } ]
  },
  "withTags" : null,
  "comments" : [ {
    "metadata" : null,
    "id" : "425827077568461",
    "from" : {
      "id" : "10206034239078191",
      "name" : "Budhi Yulianto"
    },
    "message" : "Makasih atas infonya pak Hendy Irawan, saya coba evaluasi dengan serius, soalnya waktu saya sudah mepet",
    "createdTime" : 1421229214000,
    "likeCount" : 1,
    "userLinks" : false
  }, {
    "metadata" : null,
    "id" : "425828317568337",
    "from" : {
      "id" : "10152513803701672",
      "name" : "Hendy Irawan"
    },
    "message" : "You're welcome Pak :) ontologynya lumayan modular jg jd diambil subsetnya jg bs ga harus full. In syaa Allah mulai minggu dpn aktif perkuliahan sy jd sering ngampus. Klo ingin diskusi boleh2 aja Pak, kebetulan sy udah ada gambaran dikit visi Pak Marzuki dan Pak Wahyudi beserta tim S1 Lumen.. jd bs align dgn kebutuhan integrasi",
    "createdTime" : 1421229549000,
    "likeCount" : 0,
    "userLinks" : false
  }, {
    "metadata" : null,
    "id" : "425829640901538",
    "from" : {
      "id" : "10152634063888963",
      "name" : "Wahyudi Dan Anasari"
    },
    "message" : "Lanjutkan mas Hendy Irawan...semngat pak Budhi Yulianto..",
    "createdTime" : 1421229928000,
    "likeCount" : 1,
    "userLinks" : false
  }, {
    "metadata" : null,
    "id" : "425834154234420",
    "from" : {
      "id" : "10203857869225494",
      "name" : "Febru Wasono"
    },
    "message" : "Namanya kyk panggilan anaku \"Yago\" ... :d \n\n*jd tertarik Untuk mengimplementasikan nya di data pengguna hp, vlr, community vas services",
    "createdTime" : 1421231395000,
    "likeCount" : 1,
    "userLinks" : false
  }, {
    "metadata" : null,
    "id" : "425874254230410",
    "from" : {
      "id" : "10203536415231264",
      "name" : "Marzuki Syahfirin"
    },
    "message" : "semangaat pak budhi, ini mas hendy support kita, seperti yang kita bicarakan beberapa hari yang lalu, karna waktu terus berjalan.",
    "createdTime" : 1421240398000,
    "likeCount" : 1,
    "userLinks" : false
  }, {
    "metadata" : null,
    "id" : "427249987426170",
    "from" : {
      "id" : "10206034239078191",
      "name" : "Budhi Yulianto"
    },
    "message" : "Ontology YAGO dimasukkan dalam semantic database lewat Neo4j DB engine, sebuah ide yang brilian. Makasih atas masukan pak Hendy Irawan",
    "createdTime" : 1421551802000,
    "likeCount" : 1,
    "userLinks" : false
  } ],
  "objectId" : null,
  "application" : null,
  "createdTime" : 1421629664000,
  "updatedTime" : 1421629664000
}
*/

                    // TODO: depends on https://issues.apache.org/jira/browse/CAMEL-8257
//                    final facebookHome = getContext().getEndpoint("facebook://home", FacebookEndpoint.class)
//                    facebookHome.configuration.setOAuthAppId(it.facebookSys.facebookAppId)
//                    facebookHome.configuration.setOAuthAppSecret(it.facebookSys.facebookAppSecret)
//                    facebookHome.configuration.setOAuthAccessToken(it.facebookSys.facebookAccessToken)

                    from(facebookHome).process {
                        it.in.body = mapper.writeValueAsString(it.in.body)
                        log.debug('Headers: {}', it.in.headers)
                        log.debug('Body: {}', it.in.body)
                    }.to("log:lumen." + it.id)
                }
            }
        }
    }

}

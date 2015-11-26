package org.lskk.lumen.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.FluentIterable;
import facebook4j.Post;
import org.apache.camel.Exchange;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.facebook.FacebookEndpoint;
import org.apache.camel.component.twitter.TwitterEndpoint;
import org.joda.time.DateTime;
import org.lskk.lumen.core.*;
import org.lskk.lumen.core.util.AsError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import twitter4j.DirectMessage;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Created by ceefour on 29/10/2015.
 */
@Component
public class FacebookHomeRouter extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(FacebookHomeRouter.class);

    @Inject
    protected AgentRepository agentRepo;
    @Inject
    protected ToJson toJson;
    @Inject
    private ObjectMapper mapper;
    @Inject
    private AsError asError;

    @Override
    public void configure() throws Exception {
        onException(Exception.class).bean(asError).bean(toJson).handled(true);
        errorHandler(new LoggingErrorHandlerBuilder(log));
//                from("timer:1").to("log:timer")
//                final facebook = getContext().getComponent("facebook")
//                final facebookHome = getContext().getEndpoint("facebook://home", FacebookEndpoint.class)
        final long sometimeAgo = new DateTime().minusDays(1).getMillis() / 1000l;
        FluentIterable.from(agentRepo.findAll())
                .filter(it -> Optional.ofNullable(it.getFacebookSys()).map(FacebookSysConfig::getFacebookAppSecret).isPresent())
                .forEach(ag -> {

                    final String facebookHomeUri = UriComponentsBuilder.fromUriString("facebook://home?consumer.delay=15000&oAuthAppId={apiKey}&oAuthAppSecret={apiSecret}&oAuthAccessToken={accessToken}&reading.since={readingSince}&reading.limit=20")
                            .buildAndExpand(ag.getFacebookSys().getFacebookAppId(), ag.getFacebookSys().getFacebookAppSecret(), ag.getFacebookSys().getFacebookAccessToken(), sometimeAgo).toUriString();
                    final FacebookEndpoint facebookHome = getContext().getEndpoint(facebookHomeUri, FacebookEndpoint.class);
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

//                    from(facebookHome).bean(toJson).process((Exchange it) -> {
//                      log.debug("Headers: {}", it.getIn().headers)
////                      log.debug("Body: {}", it.getIn().body)
//                    }.to("log:" + Channel.SOCIAL_PERCEPTION.key(it.getId()))
                    from(facebookHome).process((Exchange it) -> {
                        final Post fbPost = (Post) it.getIn().getBody();
                        final StatusUpdate statusUpdate = new StatusUpdate();
                        statusUpdate.setThingId(fbPost.getId());
                        statusUpdate.setUrl("https://www.facebook.com/" + fbPost.getId());
                        statusUpdate.setFrom(new Person());
                        statusUpdate.getFrom().setThingId(fbPost.getFrom().getId());
                        statusUpdate.getFrom().setName(fbPost.getFrom().getName());
                        statusUpdate.getFrom().setUrl("https://www.facebook.com/" + fbPost.getFrom().getId());
                        statusUpdate.getFrom().setPhoto(new ImageObject());
                        statusUpdate.getFrom().getPhoto().setUrl("https://graph.facebook.com/" + fbPost.getFrom().getId() + "/picture");
                        statusUpdate.setMessage(fbPost.getMessage() != null ? fbPost.getMessage() : fbPost.getStory());
                        statusUpdate.setDateCreated(new DateTime(fbPost.getCreatedTime()));
                        statusUpdate.setDatePublished(new DateTime(fbPost.getCreatedTime()));
                        statusUpdate.setDateModified(fbPost.getUpdatedTime() != null ? new DateTime(fbPost.getUpdatedTime()) : null);
                        statusUpdate.setChannel(new SocialChannel());
                        statusUpdate.getChannel().setThingId("facebook");
                        statusUpdate.getChannel().setName("Facebook");
                        it.getIn().setBody(statusUpdate);
                    }).bean(toJson)
                            .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&skipQueueDeclare=true&routingKey=" + AvatarChannel.SOCIAL_PERCEPTION.key(ag.getId()))
                            .to("log:" + AvatarChannel.SOCIAL_PERCEPTION.key(ag.getId()));
                });
    }
}

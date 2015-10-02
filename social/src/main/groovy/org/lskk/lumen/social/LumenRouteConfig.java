package org.lskk.lumen.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.FluentIterable;
import com.rabbitmq.client.ConnectionFactory;
import facebook4j.Post;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.facebook.FacebookEndpoint;
import org.apache.camel.component.twitter.TwitterEndpoint;
import org.joda.time.DateTime;
import org.lskk.lumen.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;
import twitter4j.DirectMessage;
import twitter4j.Status;

import javax.inject.Inject;
import java.util.Optional;

/**
 * Created by ceefour on 1/19/15.
 */
@Configuration
public class LumenRouteConfig {

    private static final Logger log = LoggerFactory.getLogger(LumenRouteConfig.class);

    @Inject
    protected AgentRepository agentRepo;
    @Inject
    protected ToJson toJson;

    @Bean
    public ConnectionFactory amqpConnFactory() {
      final ConnectionFactory connFactory = new ConnectionFactory();
      connFactory.setHost("localhost");
      connFactory.setUsername("guest");
      connFactory.setPassword("guest");
      return connFactory;
    }

    //@Bean
    public RouteBuilder facebookHomeRouteBuilder() {
        log.info("Initializing facebookHome RouteBuilder");

        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
//                from("timer:1").to("log:timer")
//                final facebook = getContext().getComponent("facebook")
//                final facebookHome = getContext().getEndpoint("facebook://home", FacebookEndpoint.class)
                final long sometimeAgo = new DateTime().minusDays(1).getMillis() / 1000l;
                FluentIterable.from(agentRepo.findAll())
                        .filter(it -> Optional.ofNullable(it.getFacebookSys()).map(FacebookSysConfig::getFacebookAppSecret).isPresent())
                        .forEach(ag -> {

                            final String facebookHomeUri = UriComponentsBuilder.fromUriString("facebook://home?consumer.delay=15000&oAuthAppId={apiKey}&oAuthAppSecret={apiSecret}&oAuthAccessToken={accessToken}&reading.since=%s&reading.limit=20")
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
                            .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + Channel.SOCIAL_PERCEPTION.key(ag.getId()))
                            .to("log:" + Channel.SOCIAL_PERCEPTION.key(ag.getId()));
                });
            }
        };
    }

    @Bean
    public RouteBuilder facebookFeedRouteBuilder() {
        log.info("Initializing facebookFeed RouteBuilder");
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final long sometimeAgo = new DateTime().minusDays(1).getMillis() / 1000l;
                FluentIterable.from(agentRepo.findAll())
                        .filter(it -> Optional.ofNullable(it.getFacebookSys()).map(FacebookSysConfig::getFacebookAppSecret).isPresent())
                        .forEach(ag -> {
                            final String facebookHomeUri = UriComponentsBuilder.fromUriString("facebook://home?consumer.delay=15000&oAuthAppId={apiKey}&oAuthAppSecret={apiSecret}&oAuthAccessToken={accessToken}&reading.since={readingSince}&reading.limit=20")
                                    .buildAndExpand(ag.getFacebookSys().getFacebookAppId(), ag.getFacebookSys().getFacebookAppSecret(), ag.getFacebookSys().getFacebookAccessToken(), sometimeAgo).toUriString();
                            final FacebookEndpoint facebookHome = getContext().getEndpoint(facebookHomeUri, FacebookEndpoint.class);

                            final String commentPostEndpoint = UriComponentsBuilder.fromUriString("facebook://commentPost?oAuthAppId={apiKey}&oAuthAppSecret={apiSecret}&oAuthAccessToken={accessToken}")
                                    .buildAndExpand(ag.getFacebookSys().getFacebookAppId(), ag.getFacebookSys().getFacebookAppSecret(), ag.getFacebookSys().getFacebookAccessToken()).toUriString();
                            final ProducerTemplate producerTemplate = getContext().createProducerTemplate();
                            final StatusReplier replier = new StatusReplier(producerTemplate, commentPostEndpoint);
                            final EchoProcessor echoProcessor = new EchoProcessor();

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

                                // Echo
                                echoProcessor.processStatus(statusUpdate, replier);
                            }).bean(toJson)
                                    .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + Channel.SOCIAL_PERCEPTION.key(ag.getId()))
                                    .to("log:" + Channel.SOCIAL_PERCEPTION.key(ag.getId()));
                });
            }
        };
    }

    @Bean
    public RouteBuilder expressionRouteBuilder() {
        log.info("Initializing expression RouteBuilder");
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                FluentIterable.from(agentRepo.findAll())
                        .forEach(ag -> {
                            final String facebookFeed = UriComponentsBuilder.fromUriString("facebook://postStatusMessage?oAuthAppId={facebookAppId}&oAuthAppSecret={facebookAppSecret}&oAuthAccessToken={facebookAccessToken}")
                                    .buildAndExpand(ag.getFacebookSys().getFacebookAppId(), ag.getFacebookSys().getFacebookAppSecret(), ag.getFacebookSys().getFacebookAccessToken()).toString();
                            final String twitterTimelineUser = UriComponentsBuilder.fromUriString("twitter://timeline/user?consumerKey={twitterApiKey}&consumerSecret={twitterApiSecret}&accessToken={twitterToken}&accessTokenSecret={twitterTokenSecret}")
                                            .buildAndExpand(ag.getTwitterSys().getTwitterApiKey(), ag.getTwitterSys().getTwitterApiSecret(), ag.getTwitterSys().getTwitterToken(), ag.getTwitterSys().getTwitterTokenSecret()).toString();
                            from("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + Channel.SOCIAL_EXPRESSION.key(ag.getId()))
                                .to("log:social-expression")
                                .process((Exchange it) -> {
                                    final StatusUpdate statusUpdate=toJson.mapper.readValue((byte[]) it.getIn().getBody(),StatusUpdate.class);
                                    switch (statusUpdate.getChannel().getThingId()) {
                                        case "facebook":
                                            it.getIn().setHeader("network.getId()", "facebook");
                                            it.getIn().setHeader("CamelFacebook.message", statusUpdate.getMessage());
                                            it.getIn().setBody(null);
                                            break;
                                        case "twitter":
                                            it.getIn().setHeader("network.getId()", "twitter");
                                            it.getIn().setBody(statusUpdate.getMessage());
                                            break;
                                    }
                                }).choice()
                                        .when(header("network.getId()").isEqualTo("facebook")).to(facebookFeed).to("log:facebook-postStatusMessage")
                                        .when(header("network.getId()").isEqualTo("twitter")).to(twitterTimelineUser).to("log:twitter-timeline-user")
                                        .otherwise().to("log:expression-unknown");
                                });
            }
        };
    }

    @Bean
    public RouteBuilder twitterHomeRouteBuilder() {
        log.info("Initializing twitterHome RouteBuilder");
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                FluentIterable.from(agentRepo.findAll())
                        .filter(it -> Optional.ofNullable(it.getTwitterSys()).map(TwitterSysConfig::getTwitterTokenSecret).isPresent())
                        .forEach(ag -> {
                    final String twitterHome = UriComponentsBuilder.fromUriString("twitter://timeline/home?consumerKey={twitterApiKey}&consumerSecret={twitterApiSecret}&accessToken={twitterToken}&accessTokenSecret={twitterTokenSecret}")
                                    .buildAndExpand(ag.getTwitterSys().getTwitterApiKey(), ag.getTwitterSys().getTwitterApiSecret(), ag.getTwitterSys().getTwitterToken(), ag.getTwitterSys().getTwitterTokenSecret()).toString();
                    from(twitterHome)
                            .to("log:twitter-home")
                            .process((Exchange it) -> {
                                final Status twitterStatus = (Status) it.getIn().getBody();
                                final StatusUpdate statusUpdate = new StatusUpdate();
                                statusUpdate.setThingId(String.valueOf(twitterStatus.getId()));
                                statusUpdate.setUrl("https://twitter.com/" + twitterStatus.getUser().getScreenName() + "/statuses/" + twitterStatus.getId());
                                statusUpdate.setFrom(new Person());
                                statusUpdate.getFrom().setThingId(String.valueOf(twitterStatus.getUser().getId()));
                                statusUpdate.getFrom().setSlug(twitterStatus.getUser().getScreenName());
                                statusUpdate.getFrom().setName(twitterStatus.getUser().getName());
                                statusUpdate.getFrom().setUrl("https://twitter.com/" + twitterStatus.getUser().getScreenName());
                                statusUpdate.getFrom().setPhoto(new ImageObject());
                                statusUpdate.getFrom().getPhoto().setUrl(twitterStatus.getUser().getProfileImageURLHttps());
                                statusUpdate.setMessage(twitterStatus.getText());
                                statusUpdate.setDateCreated(new DateTime(twitterStatus.getCreatedAt()));
                                statusUpdate.setDatePublished(new DateTime(twitterStatus.getCreatedAt()));
                                statusUpdate.setDateModified(new DateTime(twitterStatus.getCreatedAt()));
                                statusUpdate.setChannel(new SocialChannel());
                                statusUpdate.getChannel().setThingId("twitter");
                                statusUpdate.getChannel().setName("Twitter");
                                it.getIn().setBody(statusUpdate);
                            }).bean(toJson)
                                    .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + Channel.SOCIAL_PERCEPTION.key(ag.getId()))
                                    .to("log:" + Channel.SOCIAL_PERCEPTION.key(ag.getId()));
                        });
            }
        };
    }

    @Bean
    public RouteBuilder twitterMentionsRouteBuilder() {
        log.info("Initializing twitterMentions RouteBuilder");
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                FluentIterable.from(agentRepo.findAll())
                        .filter(it -> Optional.ofNullable(it.getTwitterSys()).map(TwitterSysConfig::getTwitterTokenSecret).isPresent())
                        .forEach(ag -> {
                    final String twitterHome = UriComponentsBuilder.fromUriString("twitter://timeline/mentions?consumerKey={twitterApiKey}&consumerSecret={twitterApiSecret}&accessToken={twitterToken}&accessTokenSecret={twitterTokenSecret}")
                                    .buildAndExpand(ag.getTwitterSys().getTwitterApiKey(), ag.getTwitterSys().getTwitterApiSecret(), ag.getTwitterSys().getTwitterToken(), ag.getTwitterSys().getTwitterTokenSecret()).toString();
                    from(twitterHome)
                            .to("log:twitter-mentions")
                            .process((Exchange it) -> {
                                final Status twitterStatus = (Status) it.getIn().getBody();
                                final Mention mention = new Mention();
                                mention.setThingId(String.valueOf(twitterStatus.getId()));
                                mention.setUrl("https://twitter.com/" + twitterStatus.getUser().getScreenName() + "/statuses/" + twitterStatus.getId());
                                mention.setFrom(new Person());
                                mention.getFrom().setThingId(String.valueOf(twitterStatus.getUser().getId()));
                                mention.getFrom().setSlug(twitterStatus.getUser().getScreenName());
                                mention.getFrom().setName(twitterStatus.getUser().getName());
                                mention.getFrom().setUrl("https://twitter.com/" + twitterStatus.getUser().getScreenName());
                                mention.getFrom().setPhoto(new ImageObject());
                                mention.getFrom().getPhoto().setUrl(twitterStatus.getUser().getProfileImageURLHttps());
                                mention.setMessage(twitterStatus.getText());
                                mention.setDateCreated(new DateTime(twitterStatus.getCreatedAt()));
                                mention.setDatePublished(new DateTime(twitterStatus.getCreatedAt()));
                                mention.setDateModified(new DateTime(twitterStatus.getCreatedAt()));
                                mention.setChannel(new SocialChannel());
                                mention.getChannel().setThingId("twitter");
                                mention.getChannel().setName("Twitter");
                                it.getIn().setBody(mention);
                            }).bean(toJson)
                                    .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + Channel.SOCIAL_PERCEPTION.key(ag.getId()))
                                    .to("log:" + Channel.SOCIAL_PERCEPTION.key(ag.getId()));
                        });
            }
        };
    }

    @Bean
    public RouteBuilder twitterDirectMessageRouteBuilder() {
        log.info("Initializing twitterDirectMessage RouteBuilder");
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                FluentIterable.from(agentRepo.findAll())
                        .filter(it -> Optional.ofNullable(it.getTwitterSys()).map(TwitterSysConfig::getTwitterTokenSecret).isPresent())
                        .forEach(ag -> {
                            final String dmUri = UriComponentsBuilder.fromUriString("twitter://directmessage?type=polling&delay=60&consumerKey={twitterApiKey}&consumerSecret={twitterApiSecret}&accessToken={twitterToken}&accessTokenSecret={twitterTokenSecret}")
                                    .buildAndExpand(ag.getTwitterSys().getTwitterApiKey(), ag.getTwitterSys().getTwitterApiSecret(), ag.getTwitterSys().getTwitterToken(), ag.getTwitterSys().getTwitterTokenSecret()).toString();
                            final TwitterEndpoint dmEndpoint = getContext().getEndpoint(dmUri, TwitterEndpoint.class);
                            from(dmEndpoint)
                                    .to("log:twitter-directmessage")
                                    .process((Exchange it) -> {
                                        final DirectMessage directMessage = (DirectMessage) it.getIn().getBody();
                                        final PrivateMessage privateMessage = new PrivateMessage();
                                        privateMessage.setThingId(String.valueOf(directMessage.getId()));
//                        statusUpdate.setUrl("https://twitter.com/" + twitterStatus.getId()
                                        privateMessage.setFrom(new Person());
                                        privateMessage.getFrom().setThingId(String.valueOf(directMessage.getSender().getId()));
                                        privateMessage.getFrom().setSlug(directMessage.getSender().getScreenName());
                                        privateMessage.getFrom().setName(directMessage.getSender().getName());
                                        privateMessage.getFrom().setUrl("https://twitter.com/" + directMessage.getSender().getScreenName());
                                        privateMessage.getFrom().setPhoto(new ImageObject());
                                        privateMessage.getFrom().getPhoto().setUrl(directMessage.getSender().getProfileImageURLHttps());
                                        privateMessage.setMessage(directMessage.getText());
                                        privateMessage.setDateCreated(new DateTime(directMessage.getCreatedAt()));
                                        privateMessage.setDatePublished(new DateTime(directMessage.getCreatedAt()));
                                        privateMessage.setDateModified(new DateTime(directMessage.getCreatedAt()));
                                        privateMessage.setChannel(new SocialChannel());
                                        privateMessage.getChannel().setThingId("twitter");
                                        privateMessage.getChannel().setName("Twitter");
                                        it.getIn().setBody(privateMessage);
                                    }).bean(toJson)
                                            .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&routingKey=" + Channel.SOCIAL_PERCEPTION.key(ag.getId()))
                                            .to("log:" + Channel.SOCIAL_PERCEPTION.key(ag.getId()));
                        });
            }
        };
    }

}

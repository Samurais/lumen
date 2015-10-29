package org.lskk.lumen.reasoner.socmed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lskk.lumen.socmed.AgentRepository;
import org.lskk.lumen.socmed.TwitterApp;
import org.lskk.lumen.socmed.TwitterAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import twitter4j.TwitterFactory;
import twitter4j.conf.PropertyConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by ceefour on 29/10/2015.
 */
@Configuration
public class ReasonerTwitterConfig {

    private static final Logger log = LoggerFactory.getLogger(ReasonerTwitterConfig.class);
    public static final String APP_ID = "lumen";
    public static final String AGENT_ID = "arkan";

    @Inject
    private Environment env;
    @Inject
    private ObjectMapper mapper;

    @Bean
    public AgentRepository agentRepo() throws IOException {
        return new AgentRepository();
    }

    @Bean
    public TwitterApp twitterApp() throws IOException {
        return mapper.readValue(new File("config/" + APP_ID + ".TwitterApp.jsonld"), TwitterApp.class);
    }

    @Bean
    public TwitterAuthorization twitterAuthorization() throws IOException {
        return mapper.readValue(new File("config/agent/" + AGENT_ID + ".TwitterAuthorization.jsonld"), TwitterAuthorization.class);
    }

    @Bean
    public TwitterFactory twitterFactory() throws IOException {
        final Properties twitterProps = new Properties();
        twitterProps.put("oauth.consumerKey", twitterApp().getApiKey());
        twitterProps.put("oauth.consumerSecret", twitterApp().getApiSecret());
        final PropertyConfiguration twitterConf = new PropertyConfiguration(twitterProps);
        return new TwitterFactory(twitterConf);
    }

}

package org.lskk.lumen.reasoner;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.lskk.lumen.core.util.ProxyConfig;
import org.lskk.lumen.reasoner.activity.InteractionSession;
import org.lskk.lumen.reasoner.activity.Script;
import org.lskk.lumen.reasoner.activity.Scriptable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Exposed by {@link InteractionSession} and used by many {@link Script}s.
 * Created by ceefour on 08/03/2016.
 */
@Configuration
@Import(ProxyConfig.class)
public class HttpClientConfig {

    @Bean(destroyMethod = "close")
    public CloseableHttpClient httpClient() {
        return CachingHttpClients.custom().useSystemProperties().build();
    }

    @Bean
    @Scriptable
    public RestTemplate restTemplate() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient()));
    }
}

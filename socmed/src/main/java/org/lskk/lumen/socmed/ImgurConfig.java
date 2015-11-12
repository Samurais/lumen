package org.lskk.lumen.socmed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.imgur.ImgUr;
import com.github.imgur.api.upload.UploadResponse;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by ceefour on 12/11/2015.
 */
@Configuration
@EnableScheduling
public class ImgurConfig {

    private static Logger log = LoggerFactory.getLogger(ImgurConfig.class);

    static class TokenResponse {
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("refresh_token")
        String refreshToken;
        @JsonProperty("expires_in")
        Long expiresIn;
        @JsonProperty("token_type")
        String tokenType;
        @JsonProperty("account_username")
        String accountUsername;

        @Override
        public String toString() {
            return "TokenResponse{" +
                    "accessToken='" + accessToken + '\'' +
                    ", refreshToken='" + refreshToken + '\'' +
                    ", expiresIn=" + expiresIn +
                    ", tokenType='" + tokenType + '\'' +
                    ", accountUsername='" + accountUsername + '\'' +
                    '}';
        }
    }

    @Inject
    private Environment env;
    @Inject
    private CloseableHttpClient httpClient;
    @Inject
    private ObjectMapper mapper;

    private String refreshToken;
    private String apiKey;
    private String apiSecret;
    private String accessToken;

    @Bean
    public ImgUr imgUr() {
        apiKey = env.getRequiredProperty("imgur.app-id");
        apiSecret = env.getRequiredProperty("imgur.app-secret");
        refreshToken = env.getRequiredProperty("imgur.refresh-token");
        final ImgUr imgUr = new ImgUr(apiKey, apiSecret);
        return imgUr;
    }

    @Scheduled(initialDelay = 0, fixedRate = 45 * 60 * 60)
    public void refreshAccessToken() throws IOException {
        final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(ImmutableList.of(
                new BasicNameValuePair("refresh_token", refreshToken),
                new BasicNameValuePair("client_id", apiKey),
                new BasicNameValuePair("client_secret", apiSecret),
                new BasicNameValuePair("grant_type", "refresh_token")), StandardCharsets.UTF_8);
        final HttpPost post = new HttpPost("https://api.imgur.com/oauth2/token");
        post.setEntity(entity);
        final CloseableHttpResponse resp = httpClient.execute(post);
        final String content = IOUtils.toString(resp.getEntity().getContent(), StandardCharsets.UTF_8);
        log.info("Refreshing imgur token {}: {}", resp.getStatusLine(), content);
        final TokenResponse response = mapper.readValue(content, TokenResponse.class);
        log.info("Refreshed imgur token: {}", response);
        accessToken = response.accessToken;
        refreshToken = response.refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String upload(ContentType contentType, byte[] media, String description) throws IOException {
        final HttpEntity entity = MultipartEntityBuilder.create()
                .setCharset(StandardCharsets.UTF_8)
                .addBinaryBody("image", media, contentType, "image")
                .addTextBody("description", description)
                .build();
        final HttpPost post = new HttpPost("https://api.imgur.com/3/image");
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setEntity(entity);
        final CloseableHttpResponse resp = httpClient.execute(post);
        final String content = IOUtils.toString(resp.getEntity().getContent(), StandardCharsets.UTF_8);
        log.info("Uploaded image {}: {}", resp.getStatusLine(), content);
        final JsonNode response = mapper.readTree(content);
        final String imageId = response.path("data").path("id").asText();
        return imageId;
    }
}

package org.lskk.lumen.socmed;

/**
 * Created by ceefour on 1/17/15.
 */
@Deprecated
public class TwitterSysConfig {
    public String getTwitterApiKey() {
        return twitterApiKey;
    }

    public void setTwitterApiKey(String twitterApiKey) {
        this.twitterApiKey = twitterApiKey;
    }

    public String getTwitterApiSecret() {
        return twitterApiSecret;
    }

    public void setTwitterApiSecret(String twitterApiSecret) {
        this.twitterApiSecret = twitterApiSecret;
    }

    public String getTwitterToken() {
        return twitterToken;
    }

    public void setTwitterToken(String twitterToken) {
        this.twitterToken = twitterToken;
    }

    public String getTwitterTokenSecret() {
        return twitterTokenSecret;
    }

    public void setTwitterTokenSecret(String twitterTokenSecret) {
        this.twitterTokenSecret = twitterTokenSecret;
    }

    private String twitterApiKey;
    private String twitterApiSecret;
    private String twitterToken;
    private String twitterTokenSecret;
}

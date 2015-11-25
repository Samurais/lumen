package org.lskk.lumen.socmed;

/**
 * Created by ceefour on 1/17/15.
 */
@Deprecated
public class TwitterSocialConfig {
    public Long getTwitterUserId() {
        return twitterUserId;
    }

    public void setTwitterUserId(Long twitterUserId) {
        this.twitterUserId = twitterUserId;
    }

    public String getTwitterScreenName() {
        return twitterScreenName;
    }

    public void setTwitterScreenName(String twitterScreenName) {
        this.twitterScreenName = twitterScreenName;
    }

    private Long twitterUserId;
    private String twitterScreenName;
}

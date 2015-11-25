package org.lskk.lumen.socmed;

/**
 * Created by ceefour on 1/17/15.
 */
@Deprecated
public class FacebookSocialConfig {
    public String getFacebookProfileId() {
        return facebookProfileId;
    }

    public void setFacebookProfileId(String facebookProfileId) {
        this.facebookProfileId = facebookProfileId;
    }

    public String getFacebookProfileName() {
        return facebookProfileName;
    }

    public void setFacebookProfileName(String facebookProfileName) {
        this.facebookProfileName = facebookProfileName;
    }

    /**
     * Can be either a numeric ID or a username.
     */
    private String facebookProfileId;
    private String facebookProfileName;
}

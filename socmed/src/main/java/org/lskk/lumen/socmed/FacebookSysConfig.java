package org.lskk.lumen.socmed;

import groovy.transform.CompileStatic;

/**
 * Created by ceefour on 1/17/15.
 */
@CompileStatic
public class FacebookSysConfig {
    public String getFacebookAppId() {
        return facebookAppId;
    }

    public void setFacebookAppId(String facebookAppId) {
        this.facebookAppId = facebookAppId;
    }

    public String getFacebookAppSecret() {
        return facebookAppSecret;
    }

    public void setFacebookAppSecret(String facebookAppSecret) {
        this.facebookAppSecret = facebookAppSecret;
    }

    public String getFacebookAccessToken() {
        return facebookAccessToken;
    }

    public void setFacebookAccessToken(String facebookAccessToken) {
        this.facebookAccessToken = facebookAccessToken;
    }

    private String facebookAppId;
    private String facebookAppSecret;
    private String facebookAccessToken;
}

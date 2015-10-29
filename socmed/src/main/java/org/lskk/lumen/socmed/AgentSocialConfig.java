package org.lskk.lumen.socmed;

import groovy.transform.CompileStatic;

/**
 * Created by ceefour on 1/17/15.
 */
@CompileStatic
public class AgentSocialConfig {
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public FacebookSocialConfig getFacebook() {
        return facebook;
    }

    public void setFacebook(FacebookSocialConfig facebook) {
        this.facebook = facebook;
    }

    public FacebookSysConfig getFacebookSys() {
        return facebookSys;
    }

    public void setFacebookSys(FacebookSysConfig facebookSys) {
        this.facebookSys = facebookSys;
    }

    public TwitterSocialConfig getTwitter() {
        return twitter;
    }

    public void setTwitter(TwitterSocialConfig twitter) {
        this.twitter = twitter;
    }

    public TwitterSysConfig getTwitterSys() {
        return twitterSys;
    }

    public void setTwitterSys(TwitterSysConfig twitterSys) {
        this.twitterSys = twitterSys;
    }

    private String id;
    private String name;
    private String email;
    private FacebookSocialConfig facebook;
    private FacebookSysConfig facebookSys;
    private TwitterSocialConfig twitter;
    private TwitterSysConfig twitterSys;
}

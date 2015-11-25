package org.lskk.lumen.socmed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

/**
 * Created by ceefour on 1/17/15.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type")
@JsonSubTypes(@JsonSubTypes.Type(name="AgentSocialConfig", value=AgentSocialConfig.class))
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentSocialConfig implements Serializable {
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

    @Deprecated
    public TwitterSysConfig getTwitterSys() {
        return twitterSys;
    }

    @Deprecated
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

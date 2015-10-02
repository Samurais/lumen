package org.lskk.lumen.reasoner

import groovy.transform.CompileStatic

/**
 * Created by ceefour on 1/17/15.
 */
@CompileStatic
class AgentSocialConfig {

    String id
    String name
    String email
    FacebookSocialConfig facebook
    FacebookSysConfig facebookSys
    TwitterSocialConfig twitter
    TwitterSysConfig twitterSys

}

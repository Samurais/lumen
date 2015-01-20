package id.ac.itb.lumen.social

import com.google.common.base.Strings
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

import javax.annotation.PostConstruct
import javax.inject.Inject

/**
 * Created by ceefour on 1/20/15.
 */
@CompileStatic
@Configuration
class ProxyConfig {

    private static final Logger log = LoggerFactory.getLogger(ProxyConfig.class)

    @Inject
    protected Environment env

    @PostConstruct
    def init() {
        if (!Strings.isNullOrEmpty(env.getProperty('http.proxyHost'))) {
            System.setProperty('http.proxyHost', env.getProperty('http.proxyHost'))
        }
        if (!Strings.isNullOrEmpty(env.getProperty('http.proxyPort'))) {
            System.setProperty('http.proxyPort', env.getProperty('http.proxyPort'))
        }
        if (!Strings.isNullOrEmpty(env.getProperty('https.proxyHost'))) {
            System.setProperty('https.proxyHost', env.getProperty('https.proxyHost'))
        }
        if (!Strings.isNullOrEmpty(env.getProperty('https.proxyPort'))) {
            System.setProperty('https.proxyPort', env.getProperty('https.proxyPort'))
        }
        if (!Strings.isNullOrEmpty(env.getProperty('http.proxyUser'))) {
            System.setProperty('http.proxyUser', env.getProperty('http.proxyUser'))
            System.setProperty('http.proxyPassword', env.getProperty('http.proxyPassword', ''))
            log.info('Using authenticated proxy http://{}:{}@{}:{}', env.getProperty('http.proxyUser'), '********',
                    env.getProperty('http.proxyHost'), env.getProperty('http.proxyPort'))
            Authenticator.setDefault(new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            env.getProperty('http.proxyUser'),
                            env.getProperty('http.proxyPassword', '').toCharArray())
                }
            })
        } else if (!Strings.isNullOrEmpty(env.getProperty('http.proxyHost'))) {
            log.info('Using unauthenticated proxy http://{}:{}',
                    env.getProperty('http.proxyHost'), env.getProperty('http.proxyPort'))
        }
    }

}

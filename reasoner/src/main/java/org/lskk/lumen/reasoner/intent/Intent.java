package org.lskk.lumen.reasoner.intent;

import org.lskk.lumen.reasoner.ux.Channel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * An input message from User that has been parsed to a specific
 * {@link Intent} and dependency parameters.
 *
 * <p>An Intent object can be serialized as JSON-LD as sent over RabbitMQ
 * so put intent behavior outside of its subclass.</p>
 *
 * <p>Simplest intent behaviors should be possible using (1) just JavaScript
 * which calls to Lumen Foundation APIs plus Jackson, RestTemplate, etc.
 * Or (2) in MVEL, in DRL format.
 * While more complex intent behaviors can be coded (3) using Java.</p>
 *
 * <p>Examples:</p>
 *
 * From https://adapt.mycroft.ai/ :
 * <pre>
 * {
 *     "confidence": 0.61,
 *     "target": null,
 *     "Artist": "joan jett",
 *     "intent_type": "MusicIntent",
 *     "MusicVerb": "put on",
 *     "MusicKeyword": "pandora"
 * }
 * </pre>
 *
 * For https://github.com/MycroftAI/adapt/blob/master/examples/single_intent_parser.py :
 * <pre>
 * [
 *     {
 *         "@type": "Intent",
 *         "intent": "WeatherIntent",
 *         "confidence": 0.61,
 *         "parameters": {
 *             "weatherKeyword": "weather",
 *             "weatherType": "snow",
 *             "location": "Tokyo"
 *         }
 *     }
 * ]
 * </pre>
 *
 * Created by ceefour on 07/11/2015.
 */
public class Intent implements Serializable {
    private Channel channel;
    private String avatarId;
    public Map<String, Object> parameters = new HashMap<>();

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }

    /**
     * It's possible to just use the generic intent object
     * if you don't want to create your own subclass.
     * @return
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }
}

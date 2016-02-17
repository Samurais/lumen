package org.lskk.lumen.reasoner.intent;

import java.io.Serializable;

/**
 * An input message from User that has been parsed to a specific
 * {@link Intent} and dependency parameters.
 *
 * Examples:
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
 *     {
 *         "@type": "IntentReceived",
 *         "intent": "WeatherIntent",
 *         "confidence": 0.61,
 *         "parameters": {
 *             "weatherKeyword": "weather",
 *             "weatherType": "snow",
 *             "location": "Tokyo"
 *         }
 *     }
 * </pre>
 *
 * Created by ceefour on 17/02/2016.
 */
public class IntentReceived implements Serializable {

}

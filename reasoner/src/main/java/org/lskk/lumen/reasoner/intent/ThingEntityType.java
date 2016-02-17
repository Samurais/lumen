package org.lskk.lumen.reasoner.intent;

/**
 * Entity type based on Persistence's Thing database.
 *
 * Examples:
 *
 * {
 *     "@type": "IntentType",
 *     "id": "AskBirthDateIntent",
 *     "slots": [
 *         {
 *             "id": "keyword",
 *             "kind": "LITERAL",
 *             "required": "true",
 *             "literals": ["lahir"]
 *         },
 *         {
 *             "id": "person",
 *             "kind": "THING",
 *             "required": "true",
 *             "thingTypes": ["yago:wordnet_person_100007846"]
 *         }
 *     ]
 * }
 *
 * With input:
 *
 * "kapan Pak Ary lahir?"
 *
 * Results in:
 *
 * <pre>
 * [
 * {
 *     "intent_type": "AskBirthDateIntent",
 *     "confidence": 0.91,
 *     "keyword": "lahir",
 *     "person": {
 *         "nn": "lumen:Ary_Setijadi_Prihatmanto",
 *         "prefLabel": "Dr.techn. Ary Setijadi Prihatmanto, ST, MT",
 *         "isPreferredMeaningOf": "Pak Ary",
 *         "types": ["yago:wordnet_person_100007846"]
 *     }
 * }
 * ]
 * </pre>
 *
 * Created by ceefour on 17/02/2016.
 */
public class ThingEntityType extends EntityType {

    private String thingType;

    /**
     * Thing type which is an YAGO type, e.g. {@code yago:wordnet_person_100007846}.
     * @return
     */
    public String getThingType() {
        return thingType;
    }

    public void setThingType(String thingType) {
        this.thingType = thingType;
    }
}

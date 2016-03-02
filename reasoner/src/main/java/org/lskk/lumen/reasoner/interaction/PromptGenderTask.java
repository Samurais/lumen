package org.lskk.lumen.reasoner.interaction;

import com.google.common.collect.ImmutableList;
import org.lskk.lumen.core.ConversationStyle;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.neo4j.ThingLabel;

import java.util.List;

/**
 * {@code promptGender} uses a specialized subclass because labels of {@code yago:wordnet_sex_105006898}
 * do not need to consult persistence.
 * Created by ceefour on 25/02/2016.
 */
public class PromptGenderTask extends PromptTask {
    public static final List<ThingLabel> LABELS = ImmutableList.of(
            ThingLabel.forThing("yago:male", "id-ID", "pria", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:male", "id-ID", "laki-laki", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:male", "id-ID", "lelaki", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:male", "id-ID", "laki", ConversationStyle.CASUAL),
            ThingLabel.forThing("yago:male", "id-ID", "cowok", ConversationStyle.CASUAL),
            ThingLabel.forThing("yago:male", "id-ID", "cowo", ConversationStyle.CASUAL),
            ThingLabel.forThing("yago:male", "id-ID", "cow", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:male", "id-ID", "co", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:male", "id-ID", "c0w0k", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:male", "id-ID", "c0w0", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:male", "id-ID", "c0w", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:male", "id-ID", "c0", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:male", "id-ID", "jantan", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:male", "id-ID", "lanang", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:male", "id-ID", "perjaka", ConversationStyle.CASUAL),
            ThingLabel.forThing("yago:male", "id-ID", "jejaka", ConversationStyle.CASUAL),
            ThingLabel.forThing("yago:female", "id-ID", "perempuan", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:female", "id-ID", "wanita", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:female", "id-ID", "gadis", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:female", "id-ID", "cewek", ConversationStyle.CASUAL),
            ThingLabel.forThing("yago:female", "id-ID", "cewe", ConversationStyle.CASUAL),
            ThingLabel.forThing("yago:female", "id-ID", "cew", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:female", "id-ID", "ce", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:female", "id-ID", "c3w3k", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:female", "id-ID", "c3w3", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:female", "id-ID", "c3w", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:female", "id-ID", "c3", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:female", "id-ID", "wadon", ConversationStyle.SLANG),
            ThingLabel.forThing("yago:male", "en-US", "male", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:male", "en-US", "man", ConversationStyle.CASUAL),
            ThingLabel.forThing("yago:male", "en-US", "a man", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:male", "en-US", "boy", ConversationStyle.CASUAL),
            ThingLabel.forThing("yago:male", "en-US", "a boy", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:female", "en-US", "female", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:female", "en-US", "a woman", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:female", "en-US", "woman", ConversationStyle.CASUAL),
            ThingLabel.forThing("yago:female", "en-US", "a girl", ConversationStyle.FORMAL),
            ThingLabel.forThing("yago:female", "en-US", "girl", ConversationStyle.CASUAL)
    );

    @Override
    public boolean isValidStringValue(String inLanguage, String value, ConversationStyle style) {
        return LABELS.stream().anyMatch(it -> it.getValue().equalsIgnoreCase(value));
    }

    @Override
    public Object toTargetValue(String expectedType, String inLanguage, String value, ConversationStyle style) {
        return LABELS.stream().filter(it -> it.getValue().equalsIgnoreCase(value)).findAny()
                .map(it -> {
                    final Thing thing = new Thing();
                    thing.setNn(it.getThingQName());
                    thing.setPrefLabel(it.getValue());
                    thing.setPrefLabelLang(it.getInLanguage());
                    return thing;
                }).get();
    }
}
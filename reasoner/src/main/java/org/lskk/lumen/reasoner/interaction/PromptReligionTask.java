package org.lskk.lumen.reasoner.interaction;

import com.google.common.collect.ImmutableList;
import org.lskk.lumen.core.ConversationStyle;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.neo4j.ThingLabel;

import java.util.List;
import java.util.Optional;

/**
 * Created by ceefour on 26/02/2016.
 */
public class PromptReligionTask extends PromptTask {

    public enum Religion {
        ISLAM("Islam", "yago:Islam"),
        PROTESTANTISM("Protestant", "yago:Protestantism"),
        CATHOLIC_CHURCH("Catholic", "yago:Catholic_Church"),
        HINDUISM("Hindu", "yago:Hinduism"),
        BUDDHISM("Buddha", "yago:Buddhism"),
        ATHEISM("Atheism", "yago:wordnet_atheism_106223468"),
        JUDAISM("Jewish", "yago:Judaism"),
        // Meta-religions, not selectable for edit, but can be used for search/preference purposes
        CHRISTIANITY("Christianity", "yago:Christianity");

        private String title;
        private String href;
        private String sameAsUri;

        Religion(String title, String href) {
            this.title = title;
            this.href = href;
            this.sameAsUri = href.replace("yago:", "http://yago-knowledge.org/resource/");
        }

        public String getTitle() {
            return title;
        }

        public String getHref() {
            return href;
        }

        public String getSameAsUri() {
            return sameAsUri;
        }

    }

    public static final List<ThingLabel> LABELS = ImmutableList.of(
            ThingLabel.forThing(Religion.ISLAM.getHref(), null, "Islam"),
            ThingLabel.forThing(Religion.ISLAM.getHref(), null, "muslim"),
            ThingLabel.forThing(Religion.ISLAM.getHref(), null, "muslimah"),
            ThingLabel.forThing(Religion.ISLAM.getHref(), null, "ikhwan"),
            ThingLabel.forThing(Religion.ISLAM.getHref(), null, "akhwat"),
            ThingLabel.forThing(Religion.ISLAM.getHref(), null, "akahwat"),
            ThingLabel.forThing(Religion.ISLAM.getHref(), null, "Islam"),
            ThingLabel.forThing(Religion.PROTESTANTISM.getHref(), "id-ID", "Kristen"),
            ThingLabel.forThing(Religion.PROTESTANTISM.getHref(), "id-ID", "Protestan"),
            ThingLabel.forThing(Religion.PROTESTANTISM.getHref(), "id-ID", "Yesus"),
            ThingLabel.forThing(Religion.CATHOLIC_CHURCH.getHref(), "id-ID", "Katolik"),
            ThingLabel.forThing(Religion.CATHOLIC_CHURCH.getHref(), "id-ID", "Katholik"),
            ThingLabel.forThing(Religion.PROTESTANTISM.getHref(), "en-US", "Christian"),
            ThingLabel.forThing(Religion.CHRISTIANITY.getHref(), "en-US", "Christ"),
            ThingLabel.forThing(Religion.PROTESTANTISM.getHref(), "en-US", "Protestant"),
            ThingLabel.forThing(Religion.PROTESTANTISM.getHref(), "en-US", "Protestantism"),
            ThingLabel.forThing(Religion.CHRISTIANITY.getHref(), "en-US", "Jesus"),
            ThingLabel.forThing(Religion.CATHOLIC_CHURCH.getHref(), "en-US", "Catholic"),
            ThingLabel.forThing(Religion.HINDUISM.getHref(), null, "Hindu"),
            ThingLabel.forThing(Religion.HINDUISM.getHref(), null, "Hinduism"),
            ThingLabel.forThing(Religion.BUDDHISM.getHref(), null, "Buddha"),
            ThingLabel.forThing(Religion.BUDDHISM.getHref(), null, "Budda"),
            ThingLabel.forThing(Religion.BUDDHISM.getHref(), null, "Buda"),
            ThingLabel.forThing(Religion.BUDDHISM.getHref(), null, "Budha"),
            ThingLabel.forThing(Religion.BUDDHISM.getHref(), null, "Buddhism"),
            ThingLabel.forThing(Religion.BUDDHISM.getHref(), null, "Budhism"),
            ThingLabel.forThing(Religion.ATHEISM.getHref(), null, "Ateis"),
            ThingLabel.forThing(Religion.ATHEISM.getHref(), null, "Atheis"),
            ThingLabel.forThing(Religion.ATHEISM.getHref(), null, "Atheist"),
            ThingLabel.forThing(Religion.ATHEISM.getHref(), null, "Ateist"),
            ThingLabel.forThing(Religion.ATHEISM.getHref(), null, "Agnostic"),
            ThingLabel.forThing(Religion.ATHEISM.getHref(), null, "Agnostik")
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
                    thing.setPrefLabelLang(Optional.ofNullable(it.getInLanguage()).orElse(inLanguage));
                    return thing;
                }).get();
    }

}

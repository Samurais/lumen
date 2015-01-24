package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Created by ceefour on 23/01/15.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class IndexedResource {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class LocalizedLabel {
        @JsonProperty('v')
        String value
        @JsonProperty('l')
        String language

        LocalizedLabel() {
        }

        LocalizedLabel(String value, String language) {
            this.value = value
            this.language = language
        }
    }

    String href
    String prefLabel
    List<LocalizedLabel> labels
    String isPreferredMeaningOf

    void addLabel(String label, String language) {
        if (!label.equals(prefLabel)) {
            if (labels == null) {
                labels = new ArrayList<>()
            }
            labels.add(new LocalizedLabel(label, language))
        }
    }
}


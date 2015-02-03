package id.ac.itb.lumen.persistence

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import groovy.transform.CompileStatic

/**
 * Created by ceefour on 23/01/15.
 */
@CompileStatic
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type", defaultImpl=IndexedResource.class)
@JsonSubTypes(@JsonSubTypes.Type(name="Resource", value=IndexedResource.class))
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


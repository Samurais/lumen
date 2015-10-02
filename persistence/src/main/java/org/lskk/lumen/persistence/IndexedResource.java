package org.lskk.lumen.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ceefour on 23/01/15.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type", defaultImpl = IndexedResource.class)
@JsonSubTypes(@JsonSubTypes.Type(name = "Resource", value = IndexedResource.class))
public class IndexedResource {
    public void addLabel(String label, String language) {
        if (!label.equals(prefLabel)) {
            if (labels == null) {
                labels = new ArrayList<LocalizedLabel>();
            }

            labels.add(new LocalizedLabel(label, language));
        }

    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(String prefLabel) {
        this.prefLabel = prefLabel;
    }

    public List<LocalizedLabel> getLabels() {
        return labels;
    }

    public void setLabels(List<LocalizedLabel> labels) {
        this.labels = labels;
    }

    public String getIsPreferredMeaningOf() {
        return isPreferredMeaningOf;
    }

    public void setIsPreferredMeaningOf(String isPreferredMeaningOf) {
        this.isPreferredMeaningOf = isPreferredMeaningOf;
    }

    private String href;
    private String prefLabel;
    private List<LocalizedLabel> labels;
    private String isPreferredMeaningOf;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LocalizedLabel {
        public LocalizedLabel() {
        }

        public LocalizedLabel(String value, String language) {
            this.value = value;
            this.language = language;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        @JsonProperty("v")
        private String value;
        @JsonProperty("l")
        private String language;
    }
}

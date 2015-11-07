package org.lskk.lumen.reasoner.nlp;

/**
 * http://www.ecenglish.com/learnenglish/lessons/will-would-shall-should
 * Created by ceefour on 07/11/2015.
 */
public enum ModalVerb {
    WILL("will", "akan"),
    WOULD("would", "akan"),
    SHALL("shall", "akan"),
    SHOULD("should", "sebaiknya"),
    CAN("can", "dapat"),
    COULD("could", "dapat"),
    MAY("may", "mungkin"),
    MIGHT("might", "mungkin"),
    MUST("must", "harus");

    private String english;
    private String indonesian;

    ModalVerb(String english, String indonesian) {
        this.english = english;
        this.indonesian = indonesian;
    }

    public String getEnglish() {
        return english;
    }

    public String getIndonesian() {
        return indonesian;
    }
}

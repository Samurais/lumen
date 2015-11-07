package org.lskk.lumen.reasoner.nlp;

import java.io.Serializable;

/**
 * Created by ceefour on 27/10/2015.
 */
public class Verb implements Serializable {
    private String href;
    private ModalVerb modal;

    public Verb() {
    }

    public Verb(String href) {
        this.href = href;
    }

    public Verb(String href, ModalVerb modal) {
        this.href = href;
        this.modal = modal;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public ModalVerb getModal() {
        return modal;
    }

    public void setModal(ModalVerb modal) {
        this.modal = modal;
    }

    @Override
    public String toString() {
        return "Verb{" +
                href +
                '}';
    }
}

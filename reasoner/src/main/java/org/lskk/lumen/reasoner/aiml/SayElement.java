package org.lskk.lumen.reasoner.aiml;

import org.lskk.lumen.core.AudioObject;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.core.EmotionKind;
import org.lskk.lumen.core.ImageObject;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Says something in the default (input) language, or a specific language,
 * based on condition.
 * Created by ceefour on 30/11/2015.
 */
public class SayElement {
    private Locale lang;
    private Locale ifLang;
    private Boolean synthesis;
    private EmotionKind emotion;
    private List<Serializable> contents = new ArrayList<>();
    private ImageObject image;
    private AudioObject audio;

    /**
     * Language of the utterance. If {@code null}, defaults to {@link #getIfLang()},
     * then defaults to input/stimulus language.
     * @return
     */
    @XmlTransient
    public Locale getLang() {
        return lang;
    }

    public void setLang(Locale lang) {
        this.lang = lang;
    }

    @XmlAttribute(name="lang")
    public String getLangAsString() {
        return Optional.ofNullable(lang).map(Locale::toLanguageTag).orElse(null);
    }

    public void setLangAsString(String lang) {
        this.lang = Optional.ofNullable(lang).map(Locale::forLanguageTag).orElse(null);
    }

    @XmlTransient
    public Locale getIfLang() {
        return ifLang;
    }

    public void setIfLang(Locale ifLang) {
        this.ifLang = ifLang;
    }

    /**
     * Only activate this Say if language matches.
     * @return
     */
    @XmlAttribute(name="iflang")
    public String getIfLangAsString() {
        return Optional.ofNullable(ifLang).map(Locale::toLanguageTag).orElse(null);
    }

    public void setIfLangAsString(String ifLang) {
        this.ifLang = Optional.ofNullable(ifLang).map(Locale::forLanguageTag).orElse(null);
    }

    /**
     * By default, speech synthesis is used. Set to {@code false} to disable,
     * for example if you already prepare a replacement {@link CommunicateAction#getAudio()}.
     * @return
     */
    @XmlAttribute
    public Boolean getSynthesis() {
        return synthesis;
    }

    public void setSynthesis(Boolean synthesis) {
        this.synthesis = synthesis;
    }

    /**
     * Emotion used for {@link #getSynthesis()}.
     * @return
     */
    @XmlAttribute
    public EmotionKind getEmotion() {
        return emotion;
    }

    public void setEmotion(EmotionKind emotion) {
        this.emotion = emotion;
    }

    /**
     * May contain "get", e.g.
     *
     * Hi there. I was just wanting to talk to &lt;get name="name"/>.
     */
    @XmlMixed
    @XmlElementRef(name = "get", type = Get.class)
    public List<Serializable> getContents() {
        return contents;
    }

    @XmlTransient
    public String getContentsString() {
        return !contents.isEmpty() ? contents.stream().map(Object::toString).collect(Collectors.joining(" ")).trim() : "";
    }

    @XmlElement
    public ImageObject getImage() {
        return image;
    }

    public void setImage(ImageObject image) {
        this.image = image;
    }

    @XmlElement
    public AudioObject getAudio() {
        return audio;
    }

    public void setAudio(AudioObject audio) {
        this.audio = audio;
    }

    @Override
    public String toString() {
        return "SayElement{" +
                "lang=" + lang +
                ", ifLang=" + ifLang +
                ", synthesis=" + synthesis +
                ", emotion=" + emotion +
                ", contents=" + contents +
                ", image=" + image +
                ", audio=" + audio +
                '}';
    }
}

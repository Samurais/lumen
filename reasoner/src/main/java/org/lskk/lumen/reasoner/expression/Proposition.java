package org.lskk.lumen.reasoner.expression;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.lskk.lumen.core.AudioObject;
import org.lskk.lumen.core.ImageObject;
import org.lskk.lumen.reasoner.nlp.NounClause;
import org.lskk.lumen.reasoner.nlp.Verb;

import java.io.Serializable;

/**
 * Created by ceefour on 29/10/2015.
 * @see Say
 * @see <a href="http://www.wagsoft.com/Papers/Thesis/11Generation.pdf">WagSoft Sentence Generation</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="@type")
@JsonSubTypes({
        @JsonSubTypes.Type(name="Greeting", value=Greeting.class),
        @JsonSubTypes.Type(name="SpInfinite", value=SpInfinite.class),
        @JsonSubTypes.Type(name="SpoNoun", value=SpoNoun.class),
        @JsonSubTypes.Type(name="SpoAdj", value=SpoAdj.class),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Proposition implements Serializable {

    public static final SpInfinite I_DONT_UNDERSTAND;

    static {
        I_DONT_UNDERSTAND = new SpInfinite();
        I_DONT_UNDERSTAND.setSubject(NounClause.I);
        I_DONT_UNDERSTAND.setPredicate(new Verb("wn30:00588888-v"));
        I_DONT_UNDERSTAND.setPolarity(Polarity.NEGATIVE);
    }

    private Polarity polarity;
    private ImageObject image;
    private AudioObject audio;

    public Polarity getPolarity() {
        return polarity;
    }

    public void setPolarity(Polarity polarity) {
        this.polarity = polarity;
    }

    public ImageObject getImage() {
        return image;
    }

    public void setImage(ImageObject image) {
        this.image = image;
    }

    public AudioObject getAudio() {
        return audio;
    }

    public void setAudio(AudioObject audio) {
        this.audio = audio;
    }
}

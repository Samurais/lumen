package org.lskk.lumen.reasoner.expression;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
}

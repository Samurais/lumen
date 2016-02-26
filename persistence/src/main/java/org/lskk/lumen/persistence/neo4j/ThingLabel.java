package org.lskk.lumen.persistence.neo4j;

import org.apache.commons.codec.language.Metaphone;
import org.lskk.lumen.core.ConversationStyle;
import org.lskk.lumen.core.IConfidenceAware;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Transient;

import java.io.Serializable;

/**
 * Created by ceefour on 22/02/2016.
 */
@NodeEntity(label = "lumen_Label")
@Ensure("CREATE INDEX ON :lumen_Label(_partition)")
@Ensure("CREATE INDEX ON :lumen_Label(l)")
@Ensure("CREATE INDEX ON :lumen_Label(v)")
@Ensure("CREATE INDEX ON :lumen_Label(m)")
public class ThingLabel implements Serializable, IConfidenceAware {

    public static final Metaphone METAPHONE;
    static {
        METAPHONE = new Metaphone();
        METAPHONE.setMaxCodeLen(15);
    }

    @GraphId
    private Long gid;
    @Property(name = "l")
    private String inLanguage;
    @Property(name = "v")
    private String value;
    @Property(name = "m")
    private String metaphone;
    @Property(name = "_partition")
    private PartitionKey partition;
    @Property(name = "style")
    private ConversationStyle style;
    @Transient
    private String thingQName;
    @Transient
    private String propertyQName;
    @Transient
    private Float confidence;

    public ThingLabel() {
    }

    /**
     * Will also set {@link #setMetaphone(String)}
     * @param partition
     * @param inLanguage
     * @param value
     * @param propertyQName
     * @param confidence
     */
    public ThingLabel(PartitionKey partition, String inLanguage, String value, String propertyQName, Float confidence) {
        this.partition = partition;
        this.inLanguage = inLanguage;
        this.value = value;
        this.propertyQName = propertyQName;
        this.confidence = confidence;
        this.metaphone = METAPHONE.encode(value);
    }

    public static ThingLabel forThing(String thingQName, String inLanguage, String value, ConversationStyle style) {
        final ThingLabel label = new ThingLabel();
        label.setThingQName(thingQName);
        label.setInLanguage(inLanguage);
        label.setValue(value);
        //label.setConfidence(confidence);
        label.setStyle(style);
        label.setMetaphone(METAPHONE.encode(value));
        return label;
    }

    public static ThingLabel forThing(String thingQName, String inLanguage, String value) {
        final ThingLabel label = new ThingLabel();
        label.setThingQName(thingQName);
        label.setInLanguage(inLanguage);
        label.setValue(value);
        //label.setConfidence(confidence);
//        label.setStyle(style);
        label.setMetaphone(METAPHONE.encode(value));
        return label;
    }

    public Long getGid() {
        return gid;
    }

    public void setGid(Long gid) {
        this.gid = gid;
    }

    public String getInLanguage() {
        return inLanguage;
    }

    public void setInLanguage(String inLanguage) {
        this.inLanguage = inLanguage;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public PartitionKey getPartition() {
        return partition;
    }

    public void setPartition(PartitionKey partition) {
        this.partition = partition;
    }

    /**
     * Metaphone with a maximum of 15 characters.
     *
     * @return
     */
    public String getMetaphone() {
        return metaphone;
    }

    public void setMetaphone(String metaphone) {
        this.metaphone = metaphone;
    }

    /**
     * For DTO or reasoning purpose: the property, e.g. {@code rdfs:label}.
     * @return
     */
    public String getPropertyQName() {
        return propertyQName;
    }

    public void setPropertyQName(String propertyQName) {
        this.propertyQName = propertyQName;
    }

    /**
     * For DTO or reasning purpose, confidence that this label applies to the {@link Thing}.
     * @return
     */
    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public String getThingQName() {
        return thingQName;
    }

    public void setThingQName(String thingQName) {
        this.thingQName = thingQName;
    }

    public ConversationStyle getStyle() {
        return style;
    }

    public void setStyle(ConversationStyle style) {
        this.style = style;
    }

    @Override
    public String toString() {
        return "ThingLabel{" +
                "gid=" + gid +
                ", inLanguage='" + inLanguage + '\'' +
                ", value='" + value + '\'' +
                ", metaphone='" + metaphone + '\'' +
                ", partition=" + partition +
                ", propertyQName='" + propertyQName + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}

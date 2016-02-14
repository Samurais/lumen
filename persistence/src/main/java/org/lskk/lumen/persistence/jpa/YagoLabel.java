package org.lskk.lumen.persistence.jpa;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by ceefour on 13/02/2016.
 */
@Entity
public class YagoLabel implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    private YagoType type;
    private String inLanguage;
    @Column(length = 4000)
    private String value;

    public YagoLabel() {
    }

    public YagoLabel(String value, String inLanguage) {
        this.value = value;
        this.inLanguage = inLanguage;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public YagoType getType() {
        return type;
    }

    public void setType(YagoType type) {
        this.type = type;
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
}

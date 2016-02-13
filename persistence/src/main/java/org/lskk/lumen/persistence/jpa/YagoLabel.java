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
    private YagoEntity entity;
    private String inLanguage;
    @Column(length = 4000)
    private String value;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public YagoEntity getEntity() {
        return entity;
    }

    public void setEntity(YagoEntity entity) {
        this.entity = entity;
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

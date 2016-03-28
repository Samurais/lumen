package org.lskk.lumen.reasoner.quran;

import com.google.common.base.MoreObjects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * memetakan antara yang di java dan di table yang ada di database
 * Created by aina on 18/11/2015.
 */
@Entity
@Table(schema = "sanad")
public class QuranChapter implements Serializable {

    @Id
    private String id;
    private String canonicalSlug;
    private String name;
    private String slug;
    private String author;
    private String inLanguage;
    private Integer chapterNum;
    private String nameWithTashkeel;
    private String nameTransliteration_id;
//getter dan setter >> code > generate
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCanonicalSlug() {
        return canonicalSlug;
    }

    public void setCanonicalSlug(String canonicalSlug) {
        this.canonicalSlug = canonicalSlug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getInLanguage() {
        return inLanguage;
    }

    public void setInLanguage(String inLanguage) {
        this.inLanguage = inLanguage;
    }

    public Integer getChapterNum() {
        return chapterNum;
    }

    public void setChapterNum(Integer chapterNum) {
        this.chapterNum = chapterNum;
    }

    public String getNameWithTashkeel() {
        return nameWithTashkeel;
    }

    public void setNameWithTashkeel(String nameWithTashkeel) {
        this.nameWithTashkeel = nameWithTashkeel;
    }

    public String getNameTransliteration_id() {
        return nameTransliteration_id;
    }

    public void setNameTransliteration_id(String nameTransliteration_id) {
        this.nameTransliteration_id = nameTransliteration_id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues()
                .add("id", id)
                .add("canonicalSlug", canonicalSlug)
                .add("name", name)
                .add("slug", slug)
                .add("author", author)
                .add("inLanguage", inLanguage)
                .add("chapterNum", chapterNum)
                .add("nameWithTashkeel", nameWithTashkeel)
                .add("nameTransliteration_id", nameTransliteration_id)
                .toString();
    }
}

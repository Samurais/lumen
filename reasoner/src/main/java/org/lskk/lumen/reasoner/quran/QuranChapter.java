package org.lskk.lumen.reasoner.quran;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by aina on 18/11/2015.
 */
@Entity
@Table(schema = "sanad")
public class QuranChapter {

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
        return "QuranChapter{" +
                "id='" + id + '\'' +
                ", canonicalSlug='" + canonicalSlug + '\'' +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", author='" + author + '\'' +
                ", inLanguage='" + inLanguage + '\'' +
                ", chapterNum=" + chapterNum +
                ", nameWithTashkeel='" + nameWithTashkeel + '\'' +
                ", nameTransliteration_id='" + nameTransliteration_id + '\'' +
                '}';
    }
}

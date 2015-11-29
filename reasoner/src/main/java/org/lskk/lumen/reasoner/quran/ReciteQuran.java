package org.lskk.lumen.reasoner.quran;

import com.google.common.collect.ImmutableList;
import org.apache.http.annotation.Immutable;
import org.lskk.lumen.reasoner.goal.Goal;

import java.util.List;

/**
 * First, set user-provided chapter {@link #setUpChapter(String)} and verses {@link #setUpVerses(String)},
 * then resolve using {@link QuranService},
 * before can be recited.
 * Created by ceefour on 16/11/2015.
 */
public class ReciteQuran extends Goal {
    private String upChapter;
    private String upVerses;
    private Integer chapterNumber;
    private List<Integer> verseNumbers;

    /**
     * User-provided chapter name or number, free-form, e.g. "al-kahfi", "kahfi", "al kahfi", Arabic/Indonesian/English variants,
     * informal names (i.e. "alam nasroh").
     * @return
     */
    public String getUpChapter() {
        return upChapter;
    }

    public void setUpChapter(String upChapter) {
        this.upChapter = upChapter;
    }

    /**
     * User-provided verse range, free-form, e.g. "5", "5-10", "5,7,8,15-16"
     * @return
     */
    public String getUpVerses() {
        return upVerses;
    }

    public void setUpVerses(String upVerses) {
        this.upVerses = upVerses;
    }

    /**
     * Resolved chapter numbers, i.e. [1,114].
     * @return
     */
    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    /**
     * Resolved verse numbers.
     * @return
     */
    public List<Integer> getVerseNumbers() {
        return verseNumbers;
    }

    public void setVerseNumbers(List<Integer> verseNumbers) {
        this.verseNumbers = verseNumbers;
    }

    @Override
    public String toString() {
        return "ReciteQuran{" +
                "upChapter='" + upChapter + '\'' +
                ", upVerses='" + upVerses + '\'' +
                ", chapterNumber=" + chapterNumber +
                ", verseNumbers=" + verseNumbers +
                '}';
    }

}

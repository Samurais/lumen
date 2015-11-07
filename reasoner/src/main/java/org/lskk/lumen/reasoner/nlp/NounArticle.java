package org.lskk.lumen.reasoner.nlp;

/**
 * http://grammar.ccc.commnet.edu/grammar/determiners/determiners.htm
 * https://en.wikipedia.org/wiki/Article_(grammar)
 * Created by ceefour on 07/11/2015.
 */
public enum NounArticle {
    // count nouns only
    MANY("many", "banyak"),
    A_FEW("a few", "beberapa"),
    FEW("few", "sedikit"),
    SEVERAL("several", "beberapa"),
    A_COUPLE("a couple of", "dua"),
    NONE("none of the", "tiada"),
    // non-count nouns only
    NOT_MUCH("not much", "tak banyak"),
    A_LITTLE("a little", "sedikit"),
    LITTLE("little", "sedikit"),
    A_BIT("a bit of", "sedikit"),
    A_GOOD_DEAL("a good deal of", "banyak"),
    A_GREAT_DEAL("a great deal of", "banyak sekali"),
    NO("no", "tiada"),
    // both count and non-count nouns
    ALL("all of the", "semua"),
    SOME("some", "sebagian"),
    MUCH("much of the", "kebanyakan"),
    MOST("most of the", "kebanyakan"),
    ENOUGH("enough", "cukup banyak"),
    A_LOT("a lot of", "banyak"),
    LOTS("lots of", "banyak"),
    PLENTY("plenty of", "banyak"),
    A_LACK("a lack of", "kurangnya"),
    // Predeterminers,
    DOUBLE("double the", "dua kali"),
    THREE_TIMES("three times the", "tiga kali"),
    FOUR_TIMES("four times the", "empat kali"),
    FIVE_TIMES("five times the", "lima kali"),
    HALF("half the", "setengah"),
    // Intensifier
    RATHER("rather a", "cukup"),
    QUITE("quite a", "cukup"),
    WHAT("what a", "benar-benar"),
    SUCH("such a", "benar-benar"),
    // articles
    /**
     * a or an.
     */
    A("a", "sebuah"),
    THE("the", ""),
    THIS("this", "ini"),
    THESE("these", "ini"),
    THAT("that", "itu"),
    THOSE("those", "itu");

    private String english;
    private String indonesian;

    NounArticle(String english, String indonesian) {
        this.english = english;
        this.indonesian = indonesian;
    }

    public String getEnglish() {
        return english;
    }

    public String getIndonesian() {
        return indonesian;
    }
}

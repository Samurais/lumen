package org.lskk.lumen.reasoner.nlp;

/**
 * Created by ceefour on 26/10/2015.
 */
public enum Pronoun {
    I("lumen:pronoun_i", PronounPerson.FIRST, PronounNumber.SINGULAR),
    YOU("lumen:pronoun_you", PronounPerson.SECOND, PronounNumber.SINGULAR),
    WE("lumen:pronoun_we", PronounPerson.FIRST, PronounNumber.PLURAL),
    THEY("lumen:pronoun_they", PronounPerson.THIRD, PronounNumber.PLURAL),
    HE("lumen:pronoun_he", PronounPerson.THIRD, PronounNumber.SINGULAR),
    SHE("lumen:pronoun_she", PronounPerson.THIRD, PronounNumber.SINGULAR),
    SOMEONE("lumen:pronoun_someone", PronounPerson.THIRD, PronounNumber.SINGULAR),
    SOMETHING("lumen:pronoun_something", PronounPerson.THIRD, PronounNumber.SINGULAR),
    NO_ONE("lumen:pronoun_no_one", PronounPerson.THIRD, PronounNumber.SINGULAR),
    EVERYONE("lumen:pronoun_everyone", PronounPerson.THIRD, PronounNumber.SINGULAR),
    ANYONE("lumen:pronoun_anyone", PronounPerson.THIRD, PronounNumber.SINGULAR),
    NOBODY("lumen:pronoun_nobody", PronounPerson.THIRD, PronounNumber.SINGULAR),
    EVERYBODY("lumen:pronoun_everybody", PronounPerson.THIRD, PronounNumber.SINGULAR),
    ANYBODY("lumen:pronoun_anybody", PronounPerson.THIRD, PronounNumber.SINGULAR),
    ANYTHING("lumen:pronoun_anything", PronounPerson.THIRD, PronounNumber.SINGULAR),
    EVERYTHING("lumen:pronoun_everything", PronounPerson.THIRD, PronounNumber.SINGULAR);

    private String href;
    private PronounPerson person;
    private PronounNumber number;

    Pronoun(String href, PronounPerson person, PronounNumber number) {
        this.href = href;
        this.person = person;
        this.number = number;
    }

    public String getHref() {
        return href;
    }

    public PronounPerson getPerson() {
        return person;
    }

    public PronounNumber getNumber() {
        return number;
    }
}

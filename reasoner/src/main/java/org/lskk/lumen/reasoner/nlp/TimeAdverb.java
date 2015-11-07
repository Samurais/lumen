package org.lskk.lumen.reasoner.nlp;

/**
 * http://www.grammar-quizzes.com/adv_time.html
 * Created by ceefour on 07/11/2015.
 */
public enum TimeAdverb {
    YESTERDAY("yesterday", "kemarin"),
    NOW("now", "sekarang"),
    TOMORROW("tomorrow", "besok"),
    EARLIER("earlier", "sebelumnya"),
    PRESENTLY("presently", "saat ini"),
    LATER("later", "nanti"),
    TODAY("today", "hari ini"),
    TONIGHT("tonight", "nanti malam"),
    LAST_SUNDAY("last Sunday", "Minggu lalu"),
    LAST_MONDAY("last Monday", "Senin lalu"),
    LAST_TUESDAY("last Tuesday", "Selasa lalu"),
    LAST_WEDNESDAY("last Wednesday", "Rabu lalu"),
    LAST_THURSDAY("last Thursday", "Kamis lalu"),
    LAST_FRIDAY("last Friday", "Jumat lalu"),
    LAST_SATURDAY("last Saturday", "Sabtu lalu"),
    THAT_SUNDAY("that Sunday", "Minggu itu"),
    THAT_MONDAY("that Monday", "Senin itu"),
    THAT_TUESDAY("that Tuesday", "Selasa itu"),
    THAT_WEDNESDAY("that Wednesday", "Rabu itu"),
    THAT_THURSDAY("that Thursday", "Kamis itu"),
    THAT_FRIDAY("that Friday", "Jumat itu"),
    THAT_SATURDAY("that Saturday", "Sabtu itu"),
    NEXT_SUNDAY("next Sunday", "Minggu depan"),
    NEXT_MONDAY("next Monday", "Senin depan"),
    NEXT_TUESDAY("next Tuesday", "Selasa depan"),
    NEXT_WEDNESDAY("next Wednesday", "Rabu depan"),
    NEXT_THURSDAY("next Thursday", "Kamis depan"),
    NEXT_FRIDAY("next Friday", "Jumat depan"),
    NEXT_SATURDAY("next Saturday", "Sabtu depan"),
    IN_THE_MORNING("in the morning", "pagi"),
    AT_NOON("at noon", "siang"),
    IN_THE_AFTERNOON("in the afternoon", "siang"),
    IN_THE_EVENING("in the evening", "malam"),
    AT_MIDNIGHT("at midnight", "tengah malam"),
    LAST_WEEK("last week", "pekan lalu"),
    LAST_MONTH("last month", "bulan lalu"),
    LAST_YEAR("last year", "tahun lalu"),
    THIS_WEEK("this week", "pekan ini"),
    THIS_MONTH("this month", "bulan ini"),
    THIS_YEAR("this year", "tahun ini"),
    NEXT_WEEK("next week", "pekan depan"),
    NEXT_MONTH("next month", "bulan depan"),
    NEXT_YEAR("next year", "tahun depan"),
    SOMETIMES("sometimes", "kadang-kadang"),
    ALWAYS("always", "selalu"),
    NEVER("never", "tidak pernah"),
    RIGHT_NOW("right now", "sekarang juga"),
    FOREVER("forever", "selamanya"),
    YET("yet", "belum"),
    SOON("soon", "sebentar lagi"),
    STILL("still", "masih"),
    FINALLY("finally", "akhirnya");

    private String english;
    private String indonesian;

    TimeAdverb(String english, String indonesian) {
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

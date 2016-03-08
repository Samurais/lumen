with(new JavaImporter(org.lskk.lumen.reasoner.activity, org.joda.time)) {

    /**
     * Before onActivate is called, all required slots are guaranteed to be set.
     */
    function onActivate() {
        // Important: to use .last, we need to set Script.autoPoll = true in the Script.json descriptor file
        var date = inSlots.date.last; // org.joda.time.LocalDate
        // Bandung:
        var lat = inSlots.lat.last; //-6.9174639;//inSlots.measure.last;
        var lon = inSlots.lon.last; //107.6191228;//inSlots.unit.last;
        var timeZone = inSlots.timeZone.last; // org.joda.time.DateTimeZone, e.g. DateTimeZone.forID('Asia/Jakarta');
        /*
        "method" - these are the different methods identifying various schools of thought about how to compute the timings. This parameter accepts values from 0-7, as signified below:
        0 - Shia Ithna-Ashari
        1 - University of Islamic Sciences, Karachi
        2 - Islamic Society of North America (ISNA)
        3 - Muslim World League (MWL)
        4 - Umm al-Qura, Makkah
        5 - Egyptian General Authority of Survey
        7 - Institute of Geophysics, University of Tehran
        */
        var method = 4//inSlots.unit.last;
        var timestamp = (date.toDateTimeAtStartOfDay(DateTimeZone.UTC).millis / 1000).toFixed();
        log.debug("Calculating prayer time date={}({}) location=({},{}) timeZone={} method={} ...",
            date, timestamp, lat, lon, timeZone, method);
        var result = restTemplate.getForObject(
            "http://api.aladhan.com/timings/{dateTime}?latitude={lat}&longitude={lon}&timezonestring={timeZone}&method={method}",
            java.util.Map.class,
            timestamp, lat, lon, timeZone, method);
        log.info("Prayer time date={}({}) location=({},{}) timeZone={} method={} -> {}",
            date, timestamp, lat, lon, timeZone, method, result);
        var fajr = new LocalTime(result.data.timings.Fajr);
        var sunrise = new LocalTime(result.data.timings.Sunrise);
        var dhuhr = new LocalTime(result.data.timings.Dhuhr);
        var asr = new LocalTime(result.data.timings.Asr);
        var sunset = new LocalTime(result.data.timings.Sunset);
        var maghrib = new LocalTime(result.data.timings.Maghrib);
        var isha = new LocalTime(result.data.timings.Isha);
        log.info("Fajr={} Sunrise={} Dhuhr={} Asr={} Sunset={} Maghrib={} Isha={}",
            fajr, sunrise, dhuhr, asr, sunset, maghrib, isha);
        outSlots.fajr.send(fajr);
    }

}

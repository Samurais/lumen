with(new JavaImporter(org.lskk.lumen.reasoner.activity)) {

    /**
     * Before onActivate is called, all required slots are guaranteed to be set.
     */
    function onActivate() {
        // Important: to use .last, we need to set Script.autoPoll = true in the Script.json descriptor file
        var chapter  = inSlots.chapter.last;
        var verse = inSlots.verse.last;
        log.info("Reciting Al-Quran {} {}", chapter, verse);
        var communicateAction = quranService.recite(chapter, verse, null);
        pendingCommunicateActions.add(communicateAction);
    }

}

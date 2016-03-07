with(new JavaImporter(org.lskk.lumen.reasoner.activity)) {

    /**
     * Before onActivate is called, all required slots are guaranteed to be set.
     */
    function onActivate() {
        // Important: to use .last, we need to set Script.autoPoll = true in the Script.json descriptor file
        var measure = inSlots.measure.last;
        var unit = inSlots.unit.last;
        var converted = measure.to(unit);
        log.debug("Converted {} to {} -> {}", measure, unit, converted);
        outSlots.converted.send(converted);
    }

}

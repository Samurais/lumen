with(new JavaImporter(org.lskk.lumen.reasoner.activity)) {

    /**
     * Before onActivate is called, all required slots are guaranteed to be set.
     */
    function onActivate() {
        // need to get() first because slots are bounded queues
        var measure = inSlots.measure.get();
        var unit = inSlots.unit.get();
        var converted = measure.to(unit);
        log.info("Converted {} to {} = {}", measure, unit, converted);
        outSlots.converted.send(converted);
    }

}

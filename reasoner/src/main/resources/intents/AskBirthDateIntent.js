// bindings to scripting scope: this: InteractionContext, intent: Intent
function start() {
    log.info("User asks birth date of", intent.person);
    // look up the property using factService
    var wasBornOnDate = factService.getProperty(
        intent.person.nn, "yago:wasBornOnDate");
    if (null != wasBornOnDate) {
        // select which fragment to use from the view by ID
        var response = new Fragment("response");
        response.add(
            new Label("person", person),
            new Label("birthDate", wasBornOnDate));
        reply(response);
    } else {
        // built-in reply for "I don't know"
        // TODO: additional interaction for letting the user teach the robot
        interactionContext.replyDontKnow();
    }
}

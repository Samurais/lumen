package org.lskk.lumen.reasoner.intent;

import org.lskk.lumen.reasoner.ux.Fragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Inspired by Camel's {@link org.apache.camel.Exchange} + {@link org.apache.camel.ProducerTemplate} + Wicket's {@link org.apache.wicket.request.cycle.RequestCycle}.
 * Created by ceefour on 18/02/2016.
 */
public class InteractionContext {
    protected static final Logger log = LoggerFactory.getLogger(InteractionContext.class);

    private final Intent intent;
    private final Locale locale;
    private final List<Fragment> replies = new ArrayList<>();

    public InteractionContext(Intent intent, Locale locale) {
        this.intent = intent;
        this.locale = locale;
    }

    public void reply(Fragment fragment) {
        log.info("Replying with {}", fragment);
        replies.add(fragment);
    }

    public List<Fragment> getReplies() {
        return replies;
    }

    /**
     * Renders the interaction response suitable for eSpeak or Azure Speech.
     * It won't have trailing newline unless explicitly rendered by the {@link Fragment}(s).
     * @return
     */
    public String renderSsml() {
        final URL markupUrl = InteractionContext.class.getResource("/intents/" + intent.getIntentTypeId() + ".xml");
        String ssml = "";
        for (final Fragment fragment : replies) {
            if (!ssml.isEmpty()) {
                ssml += "\n";
            }
            fragment.prepareRender(locale, markupUrl);
            ssml += fragment.renderSsml();
        }
        return ssml;
    }
}
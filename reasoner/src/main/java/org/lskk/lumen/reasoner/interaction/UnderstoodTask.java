package org.lskk.lumen.reasoner.interaction;

import org.lskk.lumen.core.CommunicateAction;

import java.util.Locale;

/**
 * Used after {@link PromptTask} to reply answer user has given information.
 * Created by ceefour on 28/02/2016.
 * @todo Maybe merge with {@link CollectTask}.
 */
public class UnderstoodTask extends InteractionTask {

    @Override
    public void onStateChanged(InteractionTaskState previous, InteractionTaskState current, Locale locale, InteractionSession session) {
        if (InteractionTaskState.PENDING == previous && InteractionTaskState.ACTIVE == current) {
            getPendingCommunicateActions().add(new CommunicateAction(locale, "Oh I see.", null));
        }
    }

}

package org.lskk.lumen.reasoner.interaction;

import com.google.common.base.MoreObjects;
import org.lskk.lumen.core.CommunicateAction;

import java.util.Locale;

/**
 * Used after {@link PromptTask} to reply answer user has given information.
 * affirming: to express agreement with or commitment to; uphold; support.
 * When state changes to {@link InteractionTaskState#ACTIVE}, it will express provided {@link CommunicateAction}.
 * Created by ceefour on 28/02/2016.
 * @todo Maybe merge with {@link CollectTask}.
 */
public class AffirmationTask extends InteractionTask {

    @Override
    public void onStateChanged(InteractionTaskState previous, InteractionTaskState current, Locale locale, InteractionSession session) {
        if (InteractionTaskState.PENDING == previous && InteractionTaskState.ACTIVE == current) {
            getPendingCommunicateActions().add(new CommunicateAction(locale, "Oh I see.", null));
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .toString();
    }
}

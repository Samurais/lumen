package org.lskk.lumen.reasoner.interaction;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.RandomUtils;
import org.lskk.lumen.core.CommunicateAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Used after {@link PromptTask} to reply answer user has given information.
 * affirming: to express agreement with or commitment to; uphold; support.
 * When state changes to {@link InteractionTaskState#ACTIVE}, it will express provided {@link CommunicateAction}.
 * Created by ceefour on 28/02/2016.
 * @todo Maybe merge with {@link CollectTask}.
 */
public class AffirmationTask extends InteractionTask {

    private List<UtterancePattern> expressions = new ArrayList<>();

    public List<UtterancePattern> getExpressions() {
        return expressions;
    }

    @Override
    public void onStateChanged(InteractionTaskState previous, InteractionTaskState current, Locale locale, InteractionSession session) {
        if (InteractionTaskState.PENDING == previous && InteractionTaskState.ACTIVE == current) {
            final List<UtterancePattern> localizedExpressions = expressions.stream()
                    .filter(it -> null == it.getInLanguage() || locale.equals(Locale.forLanguageTag(it.getInLanguage())))
                    .collect(Collectors.toList());
            final UtterancePattern expression = localizedExpressions.get(RandomUtils.nextInt(0, localizedExpressions.size()));
            getPendingCommunicateActions().add(new CommunicateAction(locale, expression.getPattern(), null));
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .toString();
    }
}

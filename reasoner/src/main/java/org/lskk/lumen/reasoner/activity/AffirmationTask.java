package org.lskk.lumen.reasoner.activity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.RandomUtils;
import org.lskk.lumen.core.CommunicateAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Used after {@link PromptTask} to reply answer user has given information.
 * affirming: to express agreement with or commitment to; uphold; support.
 * When state changes to {@link ActivityState#ACTIVE}, it will express provided {@link CommunicateAction}.
 * Created by ceefour on 28/02/2016.
 * @todo Maybe merge with {@link CollectTask}.
 */
public class AffirmationTask extends Task {

    private List<UtterancePattern> expressions = new ArrayList<>();

    public List<UtterancePattern> getExpressions() {
        return expressions;
    }

    @Override
    public void onStateChanged(ActivityState previous, ActivityState current, Locale locale, InteractionSession session) {
        if (ActivityState.PENDING == previous && ActivityState.ACTIVE == current) {
            final List<UtterancePattern> localizedExpressions = expressions.stream()
                    .filter(it -> null == it.getInLanguage() || locale.equals(Locale.forLanguageTag(it.getInLanguage())))
                    .collect(Collectors.toList());
            Preconditions.checkState(!localizedExpressions.isEmpty(),
                    "Cannot get %s expression for affirmation '%s' from %s expressions: %s",
                    locale.toLanguageTag(), getPath(), expressions.size(), expressions);
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

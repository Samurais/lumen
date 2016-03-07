package org.lskk.lumen.reasoner.activity;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.RandomUtils;
import org.lskk.lumen.core.CommunicateAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Used after {@link PromptTask} to reply answer user has given information.
 * affirming: to express agreement with or commitment to; uphold; support.
 * When state changes to {@link ActivityState#ACTIVE}, it will express provided {@link CommunicateAction}.
 * Created by ceefour on 28/02/2016.
 *
 * @todo Maybe merge with {@link CollectTask}.
 */
public class AffirmationTask extends Task {

    private List<UtterancePattern> expressions = new ArrayList<>();

    public List<UtterancePattern> getExpressions() {
        return expressions;
    }

    @Override
    public void onStateChanged(ActivityState previous, ActivityState current, Locale locale, InteractionSession session) throws Exception {
        super.onStateChanged(previous, current, locale, session);
        if (ActivityState.PENDING == previous && ActivityState.ACTIVE == current) {
            final List<UtterancePattern> localizedExpressions = expressions.stream()
                    .filter(it -> null == it.getInLanguage() || locale.equals(Locale.forLanguageTag(it.getInLanguage())))
                    .collect(Collectors.toList());
            Preconditions.checkState(!localizedExpressions.isEmpty(),
                    "Cannot get %s expression for affirmation '%s' from %s expressions: %s",
                    locale.toLanguageTag(), getPath(), expressions.size(), expressions);
            final UtterancePattern expression = localizedExpressions.get(RandomUtils.nextInt(0, localizedExpressions.size()));

            // interpolate with in-slots
            final String pattern = expression.getPattern();
            StringBuffer sb = new StringBuffer();
            final Pattern SLOT_PLACEHOLDER = Pattern.compile("\\{([a-z0-9_]+)\\}", Pattern.CASE_INSENSITIVE);
            final Matcher matcher = SLOT_PLACEHOLDER.matcher(pattern);
            while (matcher.find()) {
                matcher.appendReplacement(sb, "");
                final String slotId = matcher.group(1);
                final Object slotValue = getInSlot(slotId).getLast();
                log.debug("in-slot {}.{} = {}", getPath(), slotId, slotValue);
                sb.append(String.valueOf(slotValue));
            }
            matcher.appendTail(sb);
            final String result = sb.toString();
            log.info("'{}' requesting CommunicateAction: {}", getPath(), result);

            getPendingCommunicateActions().add(new CommunicateAction(locale, result, null));

            session.complete(this, locale);
        }
    }

}

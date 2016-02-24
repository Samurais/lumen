package org.lskk.lumen.reasoner.interaction;

import org.apache.commons.lang3.RandomUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.lskk.lumen.reasoner.ReasonerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p><strong>Prompt (Ask Back + Insist/Persuade)</strong></p>
 *
 * <p>Gather information from person, with a levelof persuasion if person won't answer or just partially.</p>
 *
 * Created by ceefour on 17/02/2016.
 */
public class PromptTask extends InteractionTask {

    private static final Logger log = LoggerFactory.getLogger(PromptTask.class);

    private List<LocalizedString> askSsmls = new ArrayList<>();
    private List<UtterancePattern> utterancePatterns = new ArrayList<>();
    private String property;
    private List<String> expectedTypes;
    private String unit;

    /**
     * e.g. to ask birth date:
     *
     * <pre>
     * [
     *   {"inLanguage": "id-ID", "object": "kapan tanggal lahirmu?"},
     *   {"inLanguage": "id-ID", "object": "kamu lahir tanggal berapa?"},
     *   {"inLanguage": "en-US", "object": "when were you born?"}
     * ]
     * </pre>
     *
     * There can be several SSMLs for each language.
     * @return
     */
    public List<LocalizedString> getAskSsmls() {
        return askSsmls;
    }

    public void setAskSsmls(List<LocalizedString> askSsmls) {
        this.askSsmls = askSsmls;
    }

    /**
     * e.g. utterance to provide birth date: "aku lahir tanggal {birthDate}"@id-ID
     * @return
     */
    public List<UtterancePattern> getUtterancePatterns() {
        return utterancePatterns;
    }

    /**
     * e.g. {@code yago:wasBornOnDate}
     * @return
     */
    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * e.g. {@code xs:date}, {@code yago:wordnet_person_100007846}
     * @return
     */
    public List<String> getExpectedTypes() {
        return expectedTypes;
    }

    /**
     * Any {@link javax.measure.unit.Unit}.
     * @return
     */
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Get appropriate prompt for target language, if possible.
     * If not, returns first prompt.
     * @param locale
     * @return
     */
    public LocalizedString getPrompt(Locale locale) {
        final List<LocalizedString> matches = askSsmls.stream().filter(it -> locale.equals(Locale.forLanguageTag(it.getInLanguage())))
                .collect(Collectors.toList());
        if (!matches.isEmpty()) {
            return matches.get(RandomUtils.nextInt(0, matches.size()));
        } else {
            return askSsmls.get(0);
        }
    }

    /**
     * Checks whether an utterance matched the defined patterns for this PromptTask.
     * @param locale
     * @param utterance
     * @param scope If PromptTask is not active, use {@link org.lskk.lumen.reasoner.interaction.UtterancePattern.Scope#GLOBAL}.
     *              If PromptTask is active, use {@link org.lskk.lumen.reasoner.interaction.UtterancePattern.Scope#ANY}.
     * @return
     */
    public List<UtterancePattern> matchUtterance(Locale locale, String utterance, UtterancePattern.Scope scope) {
        final Pattern PLACEHOLDER_PATTERN = Pattern.compile("[{](?<slot>[a-z0-9_]+)[}]", Pattern.CASE_INSENSITIVE);
        final String SLOT_STRING_PATTERN;
        switch (expectedTypes.get(0)) {
            case "xs:date":
                SLOT_STRING_PATTERN = "\\d+ [a-z]+ \\d+";
                break;
            default:
                throw new ReasonerException("Unsupported type: " + expectedTypes);
        }
        final List<UtterancePattern> matches = utterancePatterns.stream()
                .filter(it -> null == it.getInLanguage() || locale == Locale.forLanguageTag(it.getInLanguage()))
                .filter(it -> UtterancePattern.Scope.ANY == scope || scope == it.getScope())
                .map(it -> {
                    // Converts "aku lahir tanggal {birthdate}" where birthDate=xs:date
                    // into regex "aku lahir tanggal (?<birthdate>\d+ [a-z]+ \d+)"
                    String real = "";
                    int lastPlainOffset = 0;
                    final Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(it.getPattern());
                    final ArrayList<String> slots = new ArrayList<>();
                    while (true) {
                        final boolean found = placeholderMatcher.find();
                        if (found) {
                            String plainPart = it.getPattern().substring(lastPlainOffset, placeholderMatcher.start());
                            real += Pattern.quote(plainPart);
                            final String slot = placeholderMatcher.group("slot");
                            slots.add(slot);
                            real += "(?<" + slot + ">" + SLOT_STRING_PATTERN + ")";
                            lastPlainOffset = placeholderMatcher.end();
                        } else {
                            String plainPart = it.getPattern().substring(lastPlainOffset, it.getPattern().length());
                            real += Pattern.quote(plainPart);
                            break;
                        }
                    }
                    final Pattern realPattern = Pattern.compile(real, Pattern.CASE_INSENSITIVE);
                    log.debug("Matching {} for \"{}\"@{} {}...", realPattern, utterance, locale.toLanguageTag(), scope);
                    final Matcher realMatcher = realPattern.matcher(utterance);
                    if (realMatcher.find()) {
                        final UtterancePattern matched = new UtterancePattern();
                        final Locale realLocale = it.getInLanguage() != null ? Locale.forLanguageTag(it.getInLanguage()) : locale;
                        matched.setInLanguage(realLocale.toLanguageTag());
                        matched.setPattern(it.getPattern());
                        matched.setScope(it.getScope());
                        matched.setActual(utterance);
                        for (final String slot : slots) {
                            final String slotString = realMatcher.group(slot);
                            matched.getSlotStrings().put(slot, slotString);
                            // convert to target value
                            switch (expectedTypes.get(0)) {
                                case "xs:date":
                                    final LocalDate localDate = DateTimeFormat.longDate().withLocale(realLocale).parseLocalDate(slotString);
                                    matched.getSlotValues().put(slot, localDate);
                                    break;
                                default:
                                    throw new ReasonerException("Unsupported type: " + expectedTypes);
                            }
                        }
                        log.debug("Matched {}", matched);
                        return matched;
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
        return matches;
    }

}

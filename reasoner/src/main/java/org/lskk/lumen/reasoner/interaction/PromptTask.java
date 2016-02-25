package org.lskk.lumen.reasoner.interaction;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.lskk.lumen.core.ConfidenceComparator;
import org.lskk.lumen.core.ConversationStyle;
import org.lskk.lumen.persistence.neo4j.ThingLabel;
import org.lskk.lumen.reasoner.ReasonerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
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
@JsonTypeInfo(property = "@type", use = JsonTypeInfo.Id.NAME, defaultImpl = PromptTask.class)
@JsonSubTypes({
    @JsonSubTypes.Type(name = "PromptTask", value = PromptTask.class),
    @JsonSubTypes.Type(name = "PromptNameTask", value = PromptNameTask.class),
    @JsonSubTypes.Type(name = "PromptGenderTask", value = PromptGenderTask.class)
})
public class PromptTask extends InteractionTask {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected static final TokenizerME TOKENIZER_ENGLISH;

    static {
        try {
            TOKENIZER_ENGLISH = new TokenizerME(new TokenizerModel(
                    PromptTask.class.getResource("/org/lskk/lumen/reasoner/en-token.bin")));
        } catch (IOException e) {
            throw new ReasonerException("Cannot initialize tokenizer", e);
        }
    }

    private String id;
    private List<QuestionTemplate> askSsmls = new ArrayList<>();
    private List<UtterancePattern> utterancePatterns = new ArrayList<>();
    private String property;
    private List<String> expectedTypes;
    private String unit;

    /**
     * Inferred from the JSON filename, e.g. {@code promptBirthDate.PromptTask.json} means the ID
     * is {@code promptBirthdate}.
     * @return
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * e.g. to ask birth date:
     * <p>
     * <pre>
     * [
     *   {"inLanguage": "id-ID", "object": "kapan tanggal lahirmu?"},
     *   {"inLanguage": "id-ID", "object": "kamu lahir tanggal berapa?"},
     *   {"inLanguage": "en-US", "object": "when were you born?"}
     * ]
     * </pre>
     * <p>
     * There can be several SSMLs for each language.
     *
     * @return
     */
    public List<QuestionTemplate> getAskSsmls() {
        return askSsmls;
    }

    public void setAskSsmls(List<QuestionTemplate> askSsmls) {
        this.askSsmls = askSsmls;
    }

    /**
     * e.g. utterance to provide birth date: "aku lahir tanggal {birthDate}"@id-ID
     *
     * @return
     */
    public List<UtterancePattern> getUtterancePatterns() {
        return utterancePatterns;
    }

    /**
     * e.g. {@code yago:wasBornOnDate}
     *
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
     *
     * @return
     */
    public List<String> getExpectedTypes() {
        return expectedTypes;
    }

    /**
     * Any {@link javax.measure.unit.Unit}.
     *
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
     *
     * @param locale
     * @return
     */
    public QuestionTemplate getPrompt(Locale locale) {
        final List<QuestionTemplate> matches = askSsmls.stream().filter(it -> locale.equals(Locale.forLanguageTag(it.getInLanguage())))
                .collect(Collectors.toList());
        if (!matches.isEmpty()) {
            return matches.get(RandomUtils.nextInt(0, matches.size()));
        } else {
            return askSsmls.get(0);
        }
    }

    /**
     * Word-tokenizes the plain-text part, quotes using {@link Pattern#quote(String)} each segment independently.
     * Each token with be separated by regex whitespace ({@code \s+}).
     * @param plainPart
     * @return
     */
    protected String plainToRegex(String plainPart) {
        final String[] tokens = TOKENIZER_ENGLISH.tokenize(plainPart);
        String result = "";
        for (final String token : tokens) {
            if (!result.isEmpty()) {
                result += "\\s+";
            }
            result += Pattern.quote(token);
        }
        // leading and/or trailing whitespace
        if (plainPart.startsWith(" ")) {
            result = "\\s+" + result;
        }
        if (plainPart.endsWith(" ")) {
            result += "\\s+";
        }
        return result;
    }

    /**
     * Checks whether an utterance matched the defined patterns for this PromptTask.
     *
     * @param locale
     * @param utterance
     * @param scope     If PromptTask is not active, use {@link org.lskk.lumen.reasoner.interaction.UtterancePattern.Scope#GLOBAL}.
     *                  If PromptTask is active, use {@link org.lskk.lumen.reasoner.interaction.UtterancePattern.Scope#ANY}.
     * @return
     */
    public List<UtterancePattern> matchUtterance(Locale locale, String utterance, UtterancePattern.Scope scope) {
        final Pattern PLACEHOLDER_PATTERN = Pattern.compile("[{](?<slot>[a-z0-9_]+)[}]", Pattern.CASE_INSENSITIVE);
        final String SLOT_STRING_PATTERN;
        switch (expectedTypes.get(0)) {
            case "xsd:string":
                SLOT_STRING_PATTERN = ".+";
                break;
            case "xs:date":
                SLOT_STRING_PATTERN = "\\d+ [a-z]+ \\d+";
                break;
            case "yago:wordnet_sex_105006898":
                SLOT_STRING_PATTERN = "[a-z0-9 -]+";
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
                            real += plainToRegex(plainPart);
                            final String slot = placeholderMatcher.group("slot");
                            slots.add(slot);
                            real += "(?<" + slot + ">" + SLOT_STRING_PATTERN + ")";
                            lastPlainOffset = placeholderMatcher.end();
                        } else {
                            String plainPart = it.getPattern().substring(lastPlainOffset, it.getPattern().length());
                            real += plainToRegex(plainPart);
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
                        matched.setStyle(it.getStyle());
                        // language-independent utterance pattern gets 0.9 multiplier
                        final float languageMultiplier = null != it.getInLanguage() ? 1f : 0.9f;
                        // GLOBAL scope has full multiplier, LOCAL scope has 0.9
                        final float scopeMultiplier = UtterancePattern.Scope.GLOBAL == it.getScope() ? 1f : 0.9f;
                        matched.setConfidence(Optional.ofNullable(it.getConfidence()).orElse(1f) * languageMultiplier * scopeMultiplier);

                        // for each slot, check if the captured slot value is valid string
                        boolean allValid = true;
                        for (final String slot : slots) {
                            final String slotString = realMatcher.group(slot);
                            matched.getSlotStrings().put(slot, slotString);
                            // convert to target value
                            switch (expectedTypes.get(0)) {
                                case "xsd:string":
                                    break;
                                case "xs:date":
                                    break;
                                case "yago:wordnet_sex_105006898":
                                    if (!isValidStringValue(matched.getInLanguage(), slotString, matched.getStyle())) {
                                        log.debug("Almost matched {} but not valid string value for {}", matched, expectedTypes);
                                        allValid = false;
                                    }
                                    break;
                                default:
                                    throw new ReasonerException("Unsupported type: " + expectedTypes);
                            }
                        }
                        if (allValid) {
                            // for each slot, convert string values into target values
                            for (final String slot : slots) {
                                final String slotString = realMatcher.group(slot);
                                matched.getSlotStrings().put(slot, slotString);
                                // convert to target value
                                switch (expectedTypes.get(0)) {
                                    case "xsd:string":
                                        matched.getSlotValues().put(slot, slotString);
                                        break;
                                    case "xs:date":
                                        final LocalDate localDate = DateTimeFormat.longDate().withLocale(realLocale).parseLocalDate(slotString);
                                        matched.getSlotValues().put(slot, localDate);
                                        break;
                                    case "yago:wordnet_sex_105006898":
                                        matched.getSlotValues().put(slot, toTargetValue(matched.getInLanguage(), slotString, matched.getStyle()));
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
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(new ConfidenceComparator())
                .collect(Collectors.toList());
        return matches;
    }

    public List<ThingLabel> getLabelsToAssert(Locale locale, String utterance, UtterancePattern.Scope scope) {
        return ImmutableList.of();
    }

    public boolean isValidStringValue(String inLanguage, String value, ConversationStyle style) {
        throw new UnsupportedOperationException();
    }

    public Object toTargetValue(String inLanguage, String value, ConversationStyle style) {
        throw new UnsupportedOperationException();
    }
}
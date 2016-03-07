package org.lskk.lumen.reasoner.activity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableList;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.core.ConversationStyle;
import org.lskk.lumen.core.IConfidence;
import org.lskk.lumen.persistence.neo4j.Literal;
import org.lskk.lumen.persistence.neo4j.ThingLabel;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.intent.Slot;

import javax.measure.Measure;
import javax.measure.unit.Unit;
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
    @JsonSubTypes.Type(name = "PromptGenderTask", value = PromptGenderTask.class),
    @JsonSubTypes.Type(name = "PromptReligionTask", value = PromptReligionTask.class),
    @JsonSubTypes.Type(name = "PromptAgeTask", value = PromptAgeTask.class)
})
public class PromptTask extends Task {

    /**
     * Minimum matched {@link UtterancePattern#getConfidence()} before the captured value
     * will be accepted to complete the {@link PromptTask} and {@link Slot#send(Object)} a packet to the out-slot.
     */
    public static final float COMPLETE_MIN_CONFIDENCE = 0.8f;

    protected static final TokenizerME TOKENIZER_ENGLISH;

    static {
        try {
            TOKENIZER_ENGLISH = new TokenizerME(new TokenizerModel(
                    PromptTask.class.getResource("/org/lskk/lumen/reasoner/en-token.bin")));
        } catch (IOException e) {
            throw new ReasonerException("Cannot initialize tokenizer", e);
        }
    }

    private List<QuestionTemplate> askSsmls = new ArrayList<>();
    private List<UtterancePattern> utterancePatterns = new ArrayList<>();
    private String property;
    private String unit;

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
     * Word-tokenizes the plain-text part, quotes using {@link Pattern#quote(String)} each segment independently.
     * Each token with be separated by regex whitespace ({@code \s+}).
     * @param plainPart
     * @return
     */
    protected final String plainToRegex(String plainPart) {
        final String[] tokens = TOKENIZER_ENGLISH.tokenize(plainPart);
        String result = "";
        for (final String token : tokens) {
            if (!result.isEmpty()) {
                // Special handling: OpenNLP English Tokenizer tokenizes "I'm" as [I, 'm]
                // which makes sense but you need to be aware of this
                if (token.startsWith("'")) {
                    result += "\\s*";
                } else {
                    result += "\\s+";
                }
            }
            result += Pattern.quote(token);
        }
        // leading and/or trailing whitespace
        if (plainPart.startsWith(" ")) {
            result = "\\s+" + result;
        }
        if (plainPart.endsWith(" ") && !plainPart.trim().isEmpty()) { // avoid " " becoming double \s+
            result += "\\s+";
        }
        log.trace("plainToRegex: \"{}\" -> \"{}\"", plainPart, result);
        return result;
    }

    @Override
    public void receiveUtterance(CommunicateAction communicateAction, InteractionSession session, Task focusedTask) {
        final List<UtterancePattern> matchedUtterancePatterns = matchUtterance(communicateAction.getInLanguage(), communicateAction.getObject(),
                focusedTask == this ? UtterancePattern.Scope.ANY : UtterancePattern.Scope.GLOBAL);
        getMatchedUtterancePatterns().clear();
        getMatchedUtterancePatterns().addAll(matchedUtterancePatterns);
        // add to queue
        final List<ThingLabel> labelsToAssert = generateLabelsToAssert(matchedUtterancePatterns);
        getLabelsToAssert().addAll(labelsToAssert);
        final List<Literal> literalsToAssert = generateLiteralsToAssert(matchedUtterancePatterns);
        getLiteralsToAssert().addAll(literalsToAssert);
        final Locale realLocale = Optional.ofNullable(communicateAction.getInLanguage()).orElse(session.getLastLocale());

        final Optional<UtterancePattern> best = matchedUtterancePatterns.stream().filter(it -> it.getConfidence() >= COMPLETE_MIN_CONFIDENCE)
                .sorted(new IConfidence.Comparator()).findFirst();
        if (best.isPresent()) {
            // retract communications
            getPendingCommunicateActions().clear();

            final Set<String> outSlotIds = getOutSlots().stream().map(Slot::getId).collect(Collectors.toSet());
            final Set<String> capturedSlotIds = best.get().getSlotValues().keySet();
            if (outSlotIds.isEmpty()) {
                log.info("{} '{}' matched \"{}\"@{} with confidence {} but not sending out-slots",
                        getClass().getSimpleName(), getPath(), best.get().getPattern(), best.get().getInLanguage(),
                        best.get().getConfidence());
                askQuestion(realLocale);
            } else if (capturedSlotIds.equals(outSlotIds)) {
                log.info("{} '{}' will be completed, matched \"{}\"@{} with confidence {} and sending out-slots {}",
                        getClass().getSimpleName(), getPath(), best.get().getPattern(), best.get().getInLanguage(),
                        best.get().getConfidence(), best.get().getSlotValues());
                best.get().getSlotValues().forEach((slotId, value) -> getOutSlot(slotId).send(value));
                session.complete(this, realLocale);
            } else {
                log.info("{} '{}' matched \"{}\"@{} with confidence {} and sending out-slots {}",
                        getClass().getSimpleName(), getPath(), best.get().getPattern(), best.get().getInLanguage(),
                        best.get().getConfidence(), best.get().getSlotValues());
                best.get().getSlotValues().forEach((slotId, value) -> getOutSlot(slotId).send(value));
                askQuestion(realLocale);
            }
        }
    }

    /**
     * Checks whether an utterance matched the defined patterns for this PromptTask.
     *
     * @param locale
     * @param utterance
     * @param scope     If PromptTask is not active, use {@link org.lskk.lumen.reasoner.activity.UtterancePattern.Scope#GLOBAL}.
     *                  If PromptTask is active, use {@link org.lskk.lumen.reasoner.activity.UtterancePattern.Scope#ANY}.
     * @return
     */
    @Override
    public List<UtterancePattern> matchUtterance(Locale locale, String utterance, UtterancePattern.Scope scope) {
        final Pattern PLACEHOLDER_PATTERN = Pattern.compile("[{](?<slot>[a-z0-9_]+)[}]", Pattern.CASE_INSENSITIVE);
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
                    int plainPartLength = 0;
                    while (true) {
                        final boolean found = placeholderMatcher.find();
                        if (found) {
                            final StringBuffer sb = new StringBuffer();
                            placeholderMatcher.appendReplacement(sb, "");
                            final String plainPart = sb.toString();
                            real += plainToRegex(plainPart);
                            plainPartLength += plainPart.length();
                            final String slotId = placeholderMatcher.group("slot");
                            slots.add(slotId);

                            final Slot slot = getOutSlot(slotId);
//                            Preconditions.checkNotNull(outSlotsLegacy.containsKey(slotId),
//                                    "Utterance \"%s\" uses slot \"%s\" but not declared in outSlots. Declared outSlots are: %s",
//                                    it.getPattern(), slotId, outSlotsLegacy.keySet());
                            final String slotStringPattern;
                            switch (slot.getThingTypes().iterator().next()) {
                                case "xsd:string":
                                    slotStringPattern = ".+";
                                    break;
                                case "xsd:integer":
                                    slotStringPattern = "[\\d-]+";
                                    break;
                                case "xs:date":
                                    slotStringPattern = "\\d+ [a-z]+ \\d+";
                                    break;
                                // this pattern is dependent on the Yago Type, not the prompt
                                case "yago:wordnet_sex_105006898":
                                    slotStringPattern = "[a-z0-9 -]+";
                                    break;
                                case "yago:wordnet_religion_105946687":
                                    slotStringPattern = "[a-z '-]+";
                                    break;
                                case "yago:yagoQuantity":
                                    slotStringPattern = "[-]?[0-9]+[.,]?[0-9]*\\s+[a-z0-9/^]+";
                                    break;
                                case "yago:wordnet_unit_of_measurement_113583724":
                                    slotStringPattern = "[a-z0-9/^]+";
                                    break;
                                default:
                                    throw new ReasonerException(String.format("Slot '%s.%s' uses unsupported type '%s'",
                                            getPath(), slotId, slot.getThingTypes()));
                            }

                            real += "(?<" + slotId + ">" + slotStringPattern + ")";
                            lastPlainOffset = placeholderMatcher.end();
                        } else {
                            break;
                        }
                    }
                    final StringBuffer sb = new StringBuffer();
                    placeholderMatcher.appendTail(sb);
                    final String plainPart = sb.toString();
                    real += plainToRegex(plainPart);
                    plainPartLength += plainPart.length();

                    final Pattern realPattern = Pattern.compile(real, Pattern.CASE_INSENSITIVE);
                    log.trace("Matching '{}' -> {} for \"{}\"@{} {}...", it.getPattern(), realPattern, utterance, locale.toLanguageTag(), scope);
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
                        // GLOBAL scope has full multiplier, LOCAL scope has 0.99. Its multiplier is quite high because
                        // we still want to match e.g. "{chapter} {verse}" which only has 1 plainPart
                        final float scopeMultiplier = UtterancePattern.Scope.GLOBAL == it.getScope() ? 1f : 0.99f;
                        // we prefer as many matching plaintext as possible, i.e. "Read Quran {Al-Baqarah}" is preferred over "Read {Quran Al-Baqarah}" over "{Read Quran Al-Baqarah}"
                        final float plainPartMultiplier = 0.9f + (Math.min(plainPartLength, 20f) / 200f);
                        // a full-utterance match has 1f confidence, each additional character is -0.01 up to max penalty of -0.1
                        final float lengthMultiplier = 1f + (-0.01f * Math.min(10, utterance.length() - realMatcher.group().length()));
                        matched.setConfidence(Optional.ofNullable(it.getConfidence()).orElse(1f) * languageMultiplier * scopeMultiplier * plainPartMultiplier
                            * lengthMultiplier);

                        // for each slot, check if the captured slot value is valid in valid format for conversion to target value
                        boolean allValid = true;
                        for (final String slotId : slots) {
                            final String slotString = realMatcher.group(slotId);
                            matched.getSlotStrings().put(slotId, slotString);
                            final Slot slot = getOutSlot(slotId);
                            switch (slot.getThingTypes().iterator().next()) {
                                case "xsd:string":
                                    break;
                                case "xsd:integer":
                                    break;
                                case "xs:date":
                                    break;
                                case "yago:yagoQuantity":
                                    try {
                                        Measure.valueOf(slotString);
                                    } catch (Exception e) {
                                        log.debug("Regex matched {} but invalid Measure format: {}", matched, e.toString());
                                        allValid = false;
                                    }
                                    break;
                                case "yago:wordnet_unit_of_measurement_113583724":
                                    try {
                                        Unit.valueOf(slotString);
                                    } catch (Exception e) {
                                        log.debug("Regex matched {} but invalid Unit format: {}", matched, e.toString());
                                        allValid = false;
                                    }
                                    break;
                                case "yago:wordnet_sex_105006898":
                                case "yago:wordnet_religion_105946687":
                                    if (!isValidStringValue(matched.getInLanguage(), slotString, matched.getStyle())) {
                                        log.debug("Regex matched {} but no enum of {} has this label", matched, slot.getThingTypes());
                                        allValid = false;
                                    }
                                    break;
                                default:
                                    throw new ReasonerException("Unsupported type: " + slot.getThingTypes());
                            }
                        }
                        if (allValid) {
                            // for each slot, convert string values into target values
                            for (final String slotId : slots) {
                                final String slotString = realMatcher.group(slotId);
                                matched.getSlotStrings().put(slotId, slotString);
                                // convert to target value
                                final Slot slot = getOutSlot(slotId);
                                matched.getSlotValues().put(slotId, toTargetValue(slot.getThingTypes().iterator().next(),
                                        matched.getInLanguage(), slotString, matched.getStyle()));
                            }
                            log.debug("Matched {} multipliers: language={} scope={} plainPart={}({})", matched,
                                    languageMultiplier, scopeMultiplier, plainPartMultiplier, plainPartLength);
                            return matched;
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(new IConfidence.Comparator())
                .collect(Collectors.toList());
        return matches;
    }

    /**
     * By default returns empty list. Override this to return assertable {@link ThingLabel}s.
     * @param utteranceMatches Matches of utterance patterns returned by {@link #matchUtterance(Locale, String, UtterancePattern.Scope)}.
     * @return
     */
    public List<ThingLabel> generateLabelsToAssert(List<UtterancePattern> utteranceMatches) {
        return ImmutableList.of();
    }

    /**
     * By default returns empty list. Override this to return assertable {@link Literal}s.
     * @param utteranceMatches Matches of utterance patterns returned by {@link #matchUtterance(Locale, String, UtterancePattern.Scope)}.
     * @return
     */
    public List<Literal> generateLiteralsToAssert(List<UtterancePattern> utteranceMatches) {
        return ImmutableList.of();
    }

    public boolean isValidStringValue(String inLanguage, String value, ConversationStyle style) {
        throw new UnsupportedOperationException();
    }

    /**
     * Convert to target value. You can override this if you have your own format.
     *
     * @param expectedType
     * @param inLanguage
     * @param value
     * @param style
     * @return
     */
    public Object toTargetValue(String expectedType, String inLanguage, String value, ConversationStyle style) {
        switch (expectedType) {
            case "xsd:string":
                return value;
            case "xsd:integer":
                return Integer.parseInt(value);
            case "xs:date":
                final LocalDate localDate = DateTimeFormat.longDate().withLocale(Locale.forLanguageTag(inLanguage)).parseLocalDate(value);
                return localDate;
            case "yago:yagoQuantity":
                return Measure.valueOf(value);
            case "yago:wordnet_unit_of_measurement_113583724":
                return Unit.valueOf(value);
            default:
                throw new ReasonerException("Unsupported type: " + expectedType);
        }
    }

    @Override
    public void onStateChanged(ActivityState previous, ActivityState current, Locale locale, InteractionSession session) throws Exception {
        super.onStateChanged(previous, current, locale, session);
        if (ActivityState.ACTIVE == current) {
            // if we don't yet have the info, express the question
            askQuestion(locale);
        }
    }

    protected void askQuestion(Locale locale) {
        // Get appropriate question for target language, if possible.
        // If not, returns first question.
        final List<QuestionTemplate> matches = askSsmls.stream().filter(it -> locale.equals(Locale.forLanguageTag(it.getInLanguage())))
                .collect(Collectors.toList());
        final QuestionTemplate questionTemplate;
        if (!matches.isEmpty()) {
            questionTemplate = matches.get(RandomUtils.nextInt(0, matches.size()));
        } else {
            questionTemplate = askSsmls.get(0);
        }
        final CommunicateAction initiative = new CommunicateAction(
                Optional.ofNullable(questionTemplate.getInLanguage()).map(Locale::forLanguageTag).orElse(locale),
                questionTemplate.getObject(), null);
        initiative.setConversationStyle(questionTemplate.getStyle());
        initiative.setUsedForSynthesis(true);
        getPendingCommunicateActions().add(initiative);
    }

}
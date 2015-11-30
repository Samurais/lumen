package org.lskk.lumen.reasoner.aiml;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.core.AudioObject;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.core.ImageObject;
import org.lskk.lumen.reasoner.ReasonerException;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.lskk.lumen.reasoner.event.UnrecognizedInput;
import org.lskk.lumen.reasoner.goal.Goal;
import org.lskk.lumen.reasoner.ux.Channel;
import org.mvel2.templates.TemplateRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 10/28/15.
 */
@Service
public class AimlService {
    public static final Locale INDONESIAN = Locale.forLanguageTag("id-ID");
    private static final Logger log = LoggerFactory.getLogger(AimlService.class);
    private static final Random RANDOM = new Random();
    private static final Ordering<MatchingCategory> TRUTH_VALUE_ORDERING = new Ordering<MatchingCategory>() {
        @Override
        public int compare(@Nullable MatchingCategory a, @Nullable MatchingCategory b) {
            return a.truthValue[1] == b.truthValue[1] ? 0 : (a.truthValue[1] > b.truthValue[1] ? -1 : 1);
        }
    };
    protected static final double BEST_MATCH_TRUTH_VALUE_THRESHOLD = 0.4;

    private List<String> resourcePatterns = new ArrayList<>(ImmutableList.of("classpath*:org/lskk/lumen/reasoner/aiml/**/*.aiml"));
    private Aiml aiml;

    public List<String> getResourcePatterns() {
        return resourcePatterns;
    }

    @PostConstruct
    public void init() throws JAXBException, IOException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(Aiml.class, Category.class, Sr.class, Template.class,
            Get.class);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(AimlService.class.getClassLoader());
        final Set<Resource> allResources = new LinkedHashSet<>();
        for (final String resourcePattern : resourcePatterns) {
            final Resource[] curResources = resolver.getResources(resourcePattern);
            log.info("Resolved {} AIML resources matching {} : {}", curResources.length, resourcePattern, curResources);
            allResources.addAll(ImmutableList.copyOf(curResources));
        }
        log.info("Loading {} AIML files: {}", allResources.size(), allResources);
        this.aiml = new Aiml();
        for (final Resource res : allResources) {
            final Aiml curAiml = (Aiml) unmarshaller.unmarshal(res.getURL());
            log.info("Loaded {} AIML categories from {}", curAiml.getCategories().size(), res);
            log.trace("{} AIML: {}", curAiml.getCategories().size(), curAiml.getCategories().stream().map(Category::toString).collect(Collectors.joining("\n")));
            aiml.getCategories().addAll(curAiml.getCategories());
        }
        log.info("Total {} AIML categories from {} resources", aiml.getCategories().size(), allResources.size());
    }

    public Aiml getAiml() {
        return aiml;
    }

    /**
     *
     * @param preparedInput Punctuation removed, trimmed, whitespace normalization, lowercased.
     * @param pattern Must be lowercase
     * @return If exact match, confidence == 1.0. If not match, confidence == 0.0.
     *      If starts with, confidence == 0.9x.
     *      If ends with, confidence == 0.8x.
     *      If contains, confidence == 0.6x.
     */
    public static MatchingCategory match(Optional<Locale> patternLocale, String preparedInput, String pattern) {
        final Locale locale = patternLocale.orElse(Locale.US);
        final float localeMultiplier = patternLocale.isPresent() ? 1f : 0.99f;
        if (pattern.equalsIgnoreCase(preparedInput)) {
            return new MatchingCategory(null, locale, pattern, new float[] {1f, 1f * localeMultiplier, 0f});
        }

        // regex match
        final List<String> patternSplit = Splitter.on(' ').trimResults().omitEmptyStrings().splitToList(pattern);
        final float truthValueMultiplier = localeMultiplier * (1f + (pattern.contains("_") ? -0.02f : -0.0f) + (pattern.contains("*") ? -0.03f : -0.0f)
                - (2f * (float)Math.exp(-pattern.length())));
        String regex = patternSplit.stream().map(it -> {
            if ("*".equals(it)) {
                return "(.+?)";
            } else if ("_".equals(it)) {
                return "(\\S+)";
            } else {
                return java.util.regex.Pattern.quote(it);
            }
        }).collect(Collectors.joining("\\s+"));
        regex = "(?<!\\w)" + regex + "(?!\\w)";
        final java.util.regex.Pattern patternRegex = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
        final Matcher matcher = patternRegex.matcher(preparedInput);
        if (matcher.find()) {
            final float[] truthValue;
            if (preparedInput.equals(matcher.group())) {
                truthValue = new float[]{1f, 1f * truthValueMultiplier, 0f};
            } else if (preparedInput.startsWith(matcher.group())) {
                truthValue = new float[]{1f, 0.8f * truthValueMultiplier, 0f};
            } else if (preparedInput.endsWith(matcher.group())) {
                truthValue = new float[]{1f, 0.7f * truthValueMultiplier, 0f};
            } else {
                truthValue = new float[]{1f, 0.6f * truthValueMultiplier, 0f};
            }
            final ImmutableList.Builder<String> groub = ImmutableList.builder();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                groub.add(matcher.group(i));
            }
            return new MatchingCategory(null, locale, pattern, truthValue, groub.build());
        }

        // starts with
        if (pattern.endsWith(" _")) {
            final String pattern2 = StringUtils.removeEnd(pattern, " _");
            if (preparedInput.startsWith(pattern2 + " ")) {
                final String group0 = StringUtils.removeStartIgnoreCase(preparedInput, pattern2).trim();
                if (!group0.contains(" ")) { // group[0] must be single word
                    return new MatchingCategory(null, locale, pattern, new float[]{1f, 0.92f, 0f}, ImmutableList.of(group0));
                }
            }
        }
        if (pattern.endsWith(" *")) {
            final String pattern2 = StringUtils.removeEnd(pattern, " *");
            if (preparedInput.startsWith(pattern2 + " ")) {
                final String group0 = StringUtils.removeStartIgnoreCase(preparedInput, pattern2).trim();
                return new MatchingCategory(null, locale, pattern, new float[] {1f, 0.91f, 0f}, ImmutableList.of(group0));
            }
        }
        if (preparedInput.startsWith(pattern + " ")) {
            final String group0 = StringUtils.removeStartIgnoreCase(preparedInput, pattern).trim();
            return new MatchingCategory(null, locale, pattern, new float[] {1f, 0.90f, 0f}, ImmutableList.of(group0));
        }
        // ends with
        if (pattern.startsWith("_ ")) {
            final String pattern2 = StringUtils.removeStart(pattern, "_ ");
            if (preparedInput.endsWith(" " + pattern2)) {
                final String group0 = StringUtils.removeEndIgnoreCase(preparedInput, pattern2).trim();
                if (!group0.contains(" ")) { // group[0] must be single word
                    return new MatchingCategory(null, locale, pattern, new float[]{1f, 0.82f, 0f}, ImmutableList.of(group0));
                }
            }
        }
        if (pattern.startsWith("* ")) {
            final String pattern2 = StringUtils.removeStart(pattern, "* ");
            if (preparedInput.endsWith(" " + pattern2)) {
                final String group0 = StringUtils.removeEndIgnoreCase(preparedInput, pattern2).trim();
                return new MatchingCategory(null, locale, pattern, new float[] {1f, 0.81f, 0f}, ImmutableList.of(group0));
            }
        }
        if (preparedInput.endsWith(" " + pattern)) {
            return new MatchingCategory(null, locale, pattern, new float[]{1f, 0.80f, 0f});
        }
        // contains
        if (pattern.endsWith(" *")) {
            final String pattern2 = StringUtils.removeEnd(pattern, " *");
            if (StringUtils.containsIgnoreCase(preparedInput, " " + pattern2 + " ")) {
                final String group0 = StringUtils.substringAfter(preparedInput, pattern2).trim();
                return new MatchingCategory(null, locale, pattern, new float[] {1f, 0.61f, 0f}, ImmutableList.of(group0));
            }
        }
        if (pattern.startsWith("* ")) {
            final String pattern2 = StringUtils.removeStart(pattern, "* ");
            if (StringUtils.containsIgnoreCase(preparedInput, " " + pattern2 + " ")) {
                final String group0 = StringUtils.substringBefore(preparedInput, pattern2).trim();
                return new MatchingCategory(null, locale, pattern, new float[] {1f, 0.61f, 0f}, ImmutableList.of(group0));
            }
        }
        if (preparedInput.contains(" " + pattern + " ")) {
            return new MatchingCategory(null, locale, pattern, new float[]{1f, 0.60f, 0f});
        }
        return new MatchingCategory(null, locale, pattern, new float[]{1f, 0f, 0f});
    }

    /**
     *
     * @param upLocale
     * @param origInput
     * @param channel
     * @param avatarId
     * @param speechInput Whether this was from speech input. If {@code true}, will set {@link CommunicateAction#setUsedForSynthesis(Boolean)}
     *  to {@code true} unless a {@link SayElement} specifically disables it.
     * @return
     */
    public AgentResponse process(@Deprecated Locale upLocale, String origInput, @Nullable Channel channel, String avatarId, boolean speechInput) {
        final CommunicateAction stimulus = new CommunicateAction(upLocale, origInput, null);

        final int MAX_SRAI_DEPTH = 32;
        int sraiDepth = 0;
        MatchingCategory bestMatch = null;
        String currentInput = origInput;
        String bestReply = null;
        while (true) {
            final List<MatchingCategory> unsorted = new ArrayList<>();
            final CharMatcher punct = CharMatcher.anyOf(",.!?:;()'\"");
            final String punctRemoved = punct.removeFrom(currentInput).trim();
            final String whitespaced = punctRemoved.replaceAll("\\s+", " ").trim();
            final String lowerCased = whitespaced.toLowerCase(upLocale);
            aiml.getCategories().forEach(cat -> {
                Optional<MatchingCategory> bestPattern = Optional.empty();
                for (final Pattern pattern : cat.getPatterns()) {
                    final Optional<Locale> patternLocale = Optional.ofNullable(pattern.getInLanguage())
                            .map(Locale::forLanguageTag);
                    final MatchingCategory match = match(patternLocale,
                            lowerCased, pattern.getContent().toLowerCase());
                    if (match.truthValue[1] > 0f && (!bestPattern.isPresent() || match.truthValue[1] > bestPattern.get().truthValue[1])) {
                        match.category = cat;
                        bestPattern = Optional.of(match);
                    }
                }
                bestPattern.ifPresent(it -> unsorted.add(it));
            });
            //matches.sort((a, b) -> a.truthValue[1] == b.truthValue[1] ? 0 : (a.truthValue[1] > b.truthValue[1] ? -1 : 1));
            final ImmutableList<MatchingCategory> matches = TRUTH_VALUE_ORDERING.immutableSortedCopy(unsorted);
            log.info("{} matched for '{}' ordered by confidence:\n{}", matches.size(), lowerCased, Joiner.on("\n").join(matches));
            Preconditions.checkState(unsorted.size() == matches.size(), "Internal error: unsorted has %s but sorted has only %s elements",
                    unsorted.size(), unsorted.size());
            bestMatch = Iterables.getFirst(matches, null);
            if (bestMatch == null) {
                // oh no!
                break;
            } else {
                if (bestMatch.category.getTemplate().getSrai() != null) {
                    if (sraiDepth < MAX_SRAI_DEPTH) {
                        // here we go again
                        currentInput = bestMatch.category.getTemplate().getSrai();
                        sraiDepth++;
                    } else {
                        throw new ReasonerException(String.format("Maximum SRAI depth (%s) exceeded for '%s'. Latest match: %s",
                                MAX_SRAI_DEPTH, origInput, bestMatch));
                    }
                } else if (bestMatch.category.getTemplate().getRandoms() != null && !bestMatch.category.getTemplate().getRandoms().isEmpty()) {
                    // pick one first
                    final Choice choice = bestMatch.category.getTemplate().getRandoms().get(
                            RANDOM.nextInt(bestMatch.category.getTemplate().getRandoms().size()) );
                    log.info("Randomly picked from {}: {}", bestMatch.category.getTemplate().getRandoms().size(),
                            choice);
                    if (choice.getSrai() != null) {
                        // here we go again
                        currentInput = choice.getSrai();
                    } else {
                        // done
                        bestReply = choice.getContentsString();
                        break;
                    }
                } else {
                    // done
                    bestReply = bestMatch.category.getTemplate().getContentsString();
                    break;
                }
            }

        }
        if (bestMatch != null && bestMatch.truthValue[1] >= BEST_MATCH_TRUTH_VALUE_THRESHOLD) {
            log.info("Best for {} -{}-> {}", stimulus, bestMatch.inLanguage.toLanguageTag(), bestReply);
            if (channel != null) {
                channel.setInLanguage(bestMatch.inLanguage);
            }

            final AgentResponse agentResponse = new AgentResponse(stimulus);
            for (final SayElement sayEl : bestMatch.category.getTemplate().getSays()) {
                if (sayEl.getIfLang() == null || bestMatch.inLanguage.equals(sayEl.getIfLang())) {
                    final CommunicateAction communicateAction = new CommunicateAction(
                            Optional.ofNullable(sayEl.getLang()).orElse(bestMatch.inLanguage), sayEl.getContentsString(), null);
                    communicateAction.setAvatarId(avatarId);
                    // speech synthesis controlled by SayElement, or by speechInput argument
                    communicateAction.setUsedForSynthesis(Optional.ofNullable(sayEl.getSynthesis()).orElse(speechInput));
                    if (sayEl.getImage() != null) {
                        try {
                            communicateAction.setImage((ImageObject) BeanUtils.cloneBean(sayEl.getImage()));
                        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                            throw new ReasonerException(e, "Cannot clone %s", sayEl.getImage());
                        }
                    }
                    if (sayEl.getAudio() != null) {
                        try {
                            communicateAction.setAudio((AudioObject) BeanUtils.cloneBean(sayEl.getAudio()));
                        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                            throw new ReasonerException(e, "Cannot clone %s", sayEl.getAudio());
                        }
                    }
                    agentResponse.getCommunicateActions().add(communicateAction);
                }
            }
            agentResponse.setStimuliLanguage(bestMatch.inLanguage);
            agentResponse.setMatchingTruthValue(bestMatch.truthValue);
            final HashMap<String, Object> goalVars = new HashMap<>();
            goalVars.put("groups", bestMatch.groups);
            for (final GoalElement goalEl : bestMatch.category.getTemplate().getGoals()) {
                final Goal goalObj;
                try {
                    final Class<Goal> goalClass = (Class<Goal>) AimlService.class.getClassLoader().loadClass(goalEl.getClazz());
                    goalObj = goalClass.newInstance();
                    goalObj.setChannel(channel);
                    goalObj.setAvatarId(avatarId);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    throw new ReasonerException(e, "Cannot create goal %s", goalEl);
                }

                for (final GoalElement.GoalProperty propDef : goalEl.getProperties()) {
                    final Object value = TemplateRuntime.eval(propDef.getValueExpression(), goalVars);
                    try {
                        PropertyUtils.setProperty(goalObj, propDef.getName(), value);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new ReasonerException(e, "Cannot set %s.%s <- %s (from: %s)",
                                goalObj.getClass().getSimpleName(), propDef.getName(), value, propDef.getValueExpression());
                    }
                }

                agentResponse.getInsertables().add(goalObj);
            }
            return agentResponse;
        } else {
            log.info("UNRECOGNIZED {}", stimulus);
            final AgentResponse agentResponse = new AgentResponse(stimulus, new UnrecognizedInput());
            agentResponse.setMatchingTruthValue(new float[] { 0f, 0f, 0f });
            return agentResponse;
        }
    }

    public static class MatchingCategory {
        public String pattern;
        public Category category;
        /**
         * 3 truth values: strength (always 1.0), confidence (0..1), count (always 0).
         */
        public float[] truthValue;
        public List<String> groups = new ArrayList<>();
        public Locale inLanguage;

        public MatchingCategory(Category category, Locale inLanguage, String pattern, float[] truthValue) {
            this.pattern = pattern;
            this.category = category;
            this.inLanguage = inLanguage;
            this.truthValue = truthValue;

        }
        public MatchingCategory(Category category, Locale inLanguage, String pattern, float[] truthValue, List<String> groups) {
            this.pattern = pattern;
            this.category = category;
            this.inLanguage = inLanguage;
            this.truthValue = truthValue;
            this.groups = groups;
        }

        @Override
        public String toString() {
            return "MatchingCategory{" +
                    inLanguage.toLanguageTag() + ":'" + pattern + '\'' +
                    ", category=" + category +
                    ", truthValue=" + Arrays.toString(truthValue) +
                    ", groups=" + groups +
                    '}';
        }
    }
}

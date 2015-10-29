package org.lskk.lumen.reasoner.aiml;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 10/28/15.
 */
@Service
public class AimlService {
    private static final Logger log = LoggerFactory.getLogger(AimlService.class);
    private Aiml aiml;

    @PostConstruct
    public void init() throws JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(Aiml.class, Category.class, Srai.class, Sr.class, Template.class,
            Get.class);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final URL url = AimlService.class.getResource("alice/salutations.aiml");
        final Aiml aiml = (Aiml) unmarshaller.unmarshal(url);
        log.info("Loaded AIML from {}: {}", url, aiml.getCategories().stream().map(Category::toString).collect(Collectors.joining("\n")));
        this.aiml = aiml;
    }

    public Aiml getAiml() {
        return aiml;
    }

    /**
     *
     * @param preparedInput Punctuation removed, trimmed, whitespace normalization, uppercased.
     * @param pattern
     * @return If exact match, confidence == 1.0. If not match, confidence == 0.0.
     *      If starts with, confidence == 0.9x.
     *      If ends with, confidence == 0.8x.
     *      If contains, confidence == 0.6x.
     */
    public static MatchingCategory match(Locale locale, String preparedInput, String pattern) {
        if (pattern.equalsIgnoreCase(preparedInput)) {
            return new MatchingCategory(null, new float[] {1f, 1f, 0f});
        }
        // starts with
        if (pattern.endsWith(" _")) {
            final String pattern2 = StringUtils.removeEnd(pattern, " _");
            if (preparedInput.startsWith(pattern2)) {
                final String group0 = StringUtils.removeStartIgnoreCase(preparedInput, pattern2).trim();
                if (!group0.contains(" ")) { // group[0] must be single word
                    return new MatchingCategory(null, new float[]{1f, 0.92f, 0f}, ImmutableList.of(group0));
                }
            }
        }
        if (pattern.endsWith(" *")) {
            final String pattern2 = StringUtils.removeEnd(pattern, " *");
            if (preparedInput.startsWith(pattern2)) {
                final String group0 = StringUtils.removeStartIgnoreCase(preparedInput, pattern2).trim();
                return new MatchingCategory(null, new float[] {1f, 0.91f, 0f}, ImmutableList.of(group0));
            }
        }
        if (preparedInput.startsWith(pattern)) {
            final String group0 = StringUtils.removeStartIgnoreCase(preparedInput, pattern).trim();
            return new MatchingCategory(null, new float[] {1f, 0.90f, 0f}, ImmutableList.of(group0));
        }
        // ends with
        if (pattern.startsWith("_ ")) {
            final String pattern2 = StringUtils.removeStart(pattern, "_ ");
            if (preparedInput.endsWith(pattern2)) {
                final String group0 = StringUtils.removeEndIgnoreCase(preparedInput, pattern2).trim();
                if (!group0.contains(" ")) { // group[0] must be single word
                    return new MatchingCategory(null, new float[]{1f, 0.82f, 0f}, ImmutableList.of(group0));
                }
            }
        }
        if (pattern.startsWith("* ")) {
            final String pattern2 = StringUtils.removeStart(pattern, "* ");
            if (preparedInput.endsWith(pattern2)) {
                final String group0 = StringUtils.removeEndIgnoreCase(preparedInput, pattern2).trim();
                return new MatchingCategory(null, new float[] {1f, 0.81f, 0f}, ImmutableList.of(group0));
            }
        }
        if (preparedInput.endsWith(pattern)) {
            return new MatchingCategory(null, new float[]{1f, 0.80f, 0f});
        }
        // contains
        if (pattern.endsWith(" *")) {
            final String pattern2 = StringUtils.removeEnd(pattern, " *");
            if (StringUtils.containsIgnoreCase(preparedInput, pattern2)) {
                final String group0 = StringUtils.substringAfter(preparedInput, pattern2).trim();
                return new MatchingCategory(null, new float[] {1f, 0.61f, 0f}, ImmutableList.of(group0));
            }
        }
        if (pattern.startsWith("* ")) {
            final String pattern2 = StringUtils.removeStart(pattern, "* ");
            if (StringUtils.containsIgnoreCase(preparedInput, pattern2)) {
                final String group0 = StringUtils.substringBefore(preparedInput, pattern2).trim();
                return new MatchingCategory(null, new float[] {1f, 0.61f, 0f}, ImmutableList.of(group0));
            }
        }
        if (preparedInput.contains(pattern)) {
            return new MatchingCategory(null, new float[]{1f, 0.60f, 0f});
        }
        return new MatchingCategory(null, new float[]{1f, 0f, 0f});
    }

    public AgentResponse process(Locale locale, String origInput) {
        final List<MatchingCategory> categories = new ArrayList<>();
        final CharMatcher punct = CharMatcher.anyOf(",.!");
        final String punctRemoved = punct.removeFrom(origInput).trim();
        final String whitespaced = punctRemoved.replaceAll("\\s+", " ").trim();
        final String upperCased = whitespaced.toUpperCase(locale);
//        aiml.getCategories().forEach(cat -> {
//
//        });
        return null;
    }

    public static class MatchingCategory {
        public String pattern;
        public Category category;
        /**
         * 3 truth values: strength (always 1.0), confidence (0..1), count (always 0).
         */
        public float[] truthValue;
        public List<String> groups = new ArrayList<>();

        public MatchingCategory(Category category, float[] truthValue) {
            this.category = category;
            this.truthValue = truthValue;

        }
        public MatchingCategory(Category category, float[] truthValue, List<String> groups) {
            this.category = category;
            this.truthValue = truthValue;
            this.groups = groups;
        }
    }
}

package org.lskk.lumen.reasoner.nlp;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Created by ceefour on 28/10/2015.
 */
@Service
public class PreferredMapper {

    public static final Locale INDONESIAN = Locale.forLanguageTag("id-ID");
    private static final Logger log = LoggerFactory.getLogger(PreferredMapper.class);

    private Map<String, Map<Locale, String>> preferredVerbs;
    private Map<String, Map<Locale, String>> preferredPhysicalEntities;
    private Map<String, Map<Locale, String>> preferredScenes;
    private Map<String, Map<Locale, String>> preferredAdjectives;

    @PostConstruct
    public void init() throws IOException {
        preferredPhysicalEntities = readPreferredCsv(PreferredMapper.class.getResource("preferred_physical_entity.csv"));
        preferredScenes = readPreferredCsv(PreferredMapper.class.getResource("preferred_scene.csv"));
        preferredVerbs = readPreferredCsv(PreferredMapper.class.getResource("preferred_verb.csv"));
        preferredAdjectives = readPreferredCsv(PreferredMapper.class.getResource("preferred_adjective.csv"));
    }

    protected Map<String, Map<Locale, String>> readPreferredCsv(URL url) throws IOException {
        final ImmutableMap.Builder<String, Map<Locale, String>> mab = ImmutableMap.builder();
        try (final CSVReader reader = new CSVReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8), ';', '"', 1)) {
            while (true) {
                final String[] line = reader.readNext();
                if (line == null || line.length <= 1) {
                    break;
                }
                Preconditions.checkState(line.length >= 3, "CSV line contains insufficient fields: %s %s", line.length, ImmutableList.copyOf(line));
                final String nodeName = line[0];
                final String preferred_en_US = line[1];
                final String preferred_id_ID = line[2];
                final ImmutableMap<Locale, String> entry = ImmutableMap.of(Locale.US, preferred_en_US, INDONESIAN, preferred_id_ID);
                mab.put(nodeName, entry);
            }
        }
        final ImmutableMap<String, Map<Locale, String>> map = mab.build();
        log.info("Loaded {} preferred synset mappings from {}", url);
        return map;
    }

    public Optional<String> getPreferred(String nodeName, Locale locale) {
        final Optional<String> physicalEntity = getPreferred(preferredPhysicalEntities, nodeName, locale);
        if (physicalEntity.isPresent()) {
            return physicalEntity;
        }
        final Optional<String> scene = getPreferred(preferredScenes, nodeName, locale);
        if (scene.isPresent()) {
            return scene;
        }
        final Optional<String> verb = getPreferred(preferredVerbs, nodeName, locale);
        if (verb.isPresent()) {
            return verb;
        }
        final Optional<String> adj = getPreferred(preferredAdjectives, nodeName, locale);
        if (adj.isPresent()) {
            return adj;
        }
        return Optional.empty();
    }

    public Optional<String> getPreferred(Map<String, Map<Locale, String>> map, String nodeName, Locale locale) {
        assert map != null;
        Preconditions.checkNotNull(nodeName, "nodeName cannot be null");
        return Optional.ofNullable(map.get(nodeName)).map(it -> it.get(locale));
    }

}

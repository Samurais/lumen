package org.lskk.lumen.reasoner.interaction;

import com.google.common.collect.ImmutableMap;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.LumenCoreConfig;
import org.lskk.lumen.core.LumenLocale;
import org.lskk.lumen.core.RabbitMqConfig;
import org.lskk.lumen.persistence.neo4j.PartitionKey;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.neo4j.ThingLabel;
import org.lskk.lumen.persistence.service.MatchingThing;
import org.lskk.lumen.reasoner.intent.Intent;
import org.lskk.lumen.reasoner.intent.InteractionContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by ceefour on 18/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = JavaScriptIntentTest.IntentConfig.class)
@SpringApplicationConfiguration(PromptTaskTest.Config.class)
public class PromptTaskTest {

    @SpringBootApplication(scanBasePackageClasses = {PromptTask.class},
        exclude = {HibernateJpaAutoConfiguration.class, DataSourceAutoConfiguration.class, JmxAutoConfiguration.class, CamelAutoConfiguration.class, GroovyTemplateAutoConfiguration.class})
    @Import({LumenCoreConfig.class})
//    @Configuration
//    @ComponentScan(basePackageClasses = {IntentExecutor.class, ThingRepository.class, YagoTypeRepository.class, FactServiceImpl.class})
//    @EnableJpaRepositories(basePackageClasses = YagoTypeRepository.class)
//    @EnableNeo4jRepositories(basePackageClasses = ThingRepository.class)
    public static class Config {

    }

    @Inject
    private PromptTaskRepository promptTaskRepo;

    @Test
    public void promptBirthDate() {
        final PromptTask promptBirthdate = promptTaskRepo.get("promptBirthdate");
        assertThat(promptBirthdate.getAskSsmls(), hasSize(greaterThan(1)));
        assertThat(promptBirthdate.getUtterancePatterns(), hasSize(greaterThan(1)));
        assertThat(promptBirthdate.getProperty(), equalTo("yago:wasBornOnDate"));
        assertThat(promptBirthdate.getExpectedTypes(), contains("xs:date"));

        assertThat(promptBirthdate.getPrompt(LumenLocale.INDONESIAN).getObject(), containsString("lahir"));
        assertThat(promptBirthdate.getPrompt(Locale.US).getObject(), containsString("born"));

        final List<UtterancePattern> matches = promptBirthdate.matchUtterance(LumenLocale.INDONESIAN, "Aku lahir tanggal 14 Desember 1983.",
                UtterancePattern.Scope.ANY);
        assertThat(matches, hasSize(2));
        assertThat(matches.get(0).getSlotStrings(), equalTo(ImmutableMap.of("birthdate", "14 Desember 1983")));
        assertThat(matches.get(1).getSlotStrings(), equalTo(ImmutableMap.of("birthdate", "14 Desember 1983")));
        assertThat(matches.get(0).getSlotValues(), equalTo(ImmutableMap.of("birthdate", new LocalDate("1983-12-14"))));
        assertThat(matches.get(1).getSlotValues(), equalTo(ImmutableMap.of("birthdate", new LocalDate("1983-12-14"))));
    }

    @Test
    public void promptName() {
        final PromptTask promptName = promptTaskRepo.get("promptName");
        assertThat(promptName, instanceOf(PromptNameTask.class));
        assertThat(promptName.getAskSsmls(), hasSize(greaterThan(1)));
        assertThat(promptName.getUtterancePatterns(), hasSize(greaterThan(1)));
        assertThat(promptName.getProperty(), equalTo("rdfs:label"));
        assertThat(promptName.getExpectedTypes(), contains("xsd:string"));

        assertThat(promptName.getPrompt(LumenLocale.INDONESIAN).getObject(), containsString("nama"));
        assertThat(promptName.getPrompt(Locale.US).getObject(), containsString("name"));

        final List<UtterancePattern> matches = promptName.matchUtterance(LumenLocale.INDONESIAN, "Namaku Hendy Irawan",
                UtterancePattern.Scope.ANY);
        assertThat(matches, hasSize(greaterThanOrEqualTo(2)));
        final List<UtterancePattern> confidentMatches = matches.stream().filter(it -> 1f == it.getConfidence()).collect(Collectors.toList());
        assertThat(confidentMatches, hasSize(greaterThanOrEqualTo(1)));
        assertThat(confidentMatches.get(0).getSlotStrings(), equalTo(ImmutableMap.of("name", "Hendy Irawan")));
        assertThat(confidentMatches.get(0).getSlotValues(), equalTo(ImmutableMap.of("name", "Hendy Irawan")));

        final List<ThingLabel> labels = promptName.getLabelsToAssert(LumenLocale.INDONESIAN, "Namaku Hendy Irawan", UtterancePattern.Scope.ANY);
        assertThat(labels, hasSize(greaterThanOrEqualTo(3)));
        final ThingLabel skos_prefLabel = labels.stream().filter(it -> "skos:prefLabel".equals(it.getPropertyQName())).findFirst().get();
        assertThat(skos_prefLabel.getValue(), equalTo("Hendy Irawan"));
        assertThat(skos_prefLabel.getMetaphone(), equalTo("HNTRWN"));
        assertThat(skos_prefLabel.getInLanguage(), equalTo("id-ID"));
        assertThat(skos_prefLabel.getPartition(), equalTo(PartitionKey.lumen_var));
        assertThat(skos_prefLabel.getConfidence(), lessThan(1f));
        final ThingLabel rdfs_label = labels.stream().filter(it -> "rdfs:label".equals(it.getPropertyQName())).findFirst().get();
        assertThat(rdfs_label.getValue(), equalTo("Hendy Irawan"));
        assertThat(rdfs_label.getMetaphone(), equalTo("HNTRWN"));
        assertThat(rdfs_label.getInLanguage(), equalTo("id-ID"));
        assertThat(rdfs_label.getPartition(), equalTo(PartitionKey.lumen_var));
        assertThat(rdfs_label.getConfidence(), equalTo(1f));
        final ThingLabel yago_hasGivenName = labels.stream().filter(it -> "yago:hasGivenName".equals(it.getPropertyQName())).findFirst().get();
        assertThat(yago_hasGivenName.getValue(), equalTo("Hendy"));
        assertThat(yago_hasGivenName.getMetaphone(), equalTo("HNT"));
        assertThat(yago_hasGivenName.getInLanguage(), equalTo("id-ID"));
        assertThat(yago_hasGivenName.getPartition(), equalTo(PartitionKey.lumen_var));
        assertThat(yago_hasGivenName.getConfidence(), equalTo(1f));
        final ThingLabel yago_hasFamilyName = labels.stream().filter(it -> "yago:hasFamilyName".equals(it.getPropertyQName())).findFirst().get();
        assertThat(yago_hasFamilyName.getValue(), equalTo("Irawan"));
        assertThat(yago_hasFamilyName.getMetaphone(), equalTo("IRWN"));
        assertThat(yago_hasFamilyName.getInLanguage(), equalTo("id-ID"));
        assertThat(yago_hasFamilyName.getPartition(), equalTo(PartitionKey.lumen_var));
        assertThat(yago_hasFamilyName.getConfidence(), equalTo(1f));
    }

    @Test
    public void promptNameSigit() {
        final PromptTask promptName = promptTaskRepo.get("promptName");
        assertThat(promptName, instanceOf(PromptNameTask.class));
        assertThat(promptName.getAskSsmls(), hasSize(greaterThan(1)));
        assertThat(promptName.getUtterancePatterns(), hasSize(greaterThan(1)));
        assertThat(promptName.getProperty(), equalTo("rdfs:label"));
        assertThat(promptName.getExpectedTypes(), contains("xsd:string"));

        assertThat(promptName.getPrompt(LumenLocale.INDONESIAN).getObject(), containsString("nama"));
        assertThat(promptName.getPrompt(Locale.US).getObject(), containsString("name"));

        final List<UtterancePattern> matches = promptName.matchUtterance(Locale.US, "I am Sigit Ari Wijanarko",
                UtterancePattern.Scope.ANY);
        assertThat(matches, hasSize(greaterThanOrEqualTo(2)));
        final List<UtterancePattern> confidentMatches = matches.stream().filter(it -> 1f == it.getConfidence()).collect(Collectors.toList());
        assertThat(confidentMatches, hasSize(greaterThanOrEqualTo(1)));
        assertThat(confidentMatches.get(0).getSlotStrings(), equalTo(ImmutableMap.of("name", "Sigit Ari Wijanarko")));
        assertThat(confidentMatches.get(0).getSlotValues(), equalTo(ImmutableMap.of("name", "Sigit Ari Wijanarko")));

        final List<ThingLabel> labels = promptName.getLabelsToAssert(Locale.US, "I am Sigit Ari Wijanarko", UtterancePattern.Scope.ANY);
        assertThat(labels, hasSize(greaterThanOrEqualTo(3)));
        final ThingLabel skos_prefLabel = labels.stream().filter(it -> "skos:prefLabel".equals(it.getPropertyQName())).findFirst().get();
        assertThat(skos_prefLabel.getValue(), equalTo("Sigit Ari Wijanarko"));
        assertThat(skos_prefLabel.getMetaphone(), equalTo("SJTRWJNRK"));
        assertThat(skos_prefLabel.getInLanguage(), equalTo("en-US"));
        assertThat(skos_prefLabel.getPartition(), equalTo(PartitionKey.lumen_var));
        assertThat(skos_prefLabel.getConfidence(), lessThan(1f));
        final ThingLabel rdfs_label = labels.stream().filter(it -> "rdfs:label".equals(it.getPropertyQName())).findFirst().get();
        assertThat(rdfs_label.getValue(), equalTo("Sigit Ari Wijanarko"));
        assertThat(rdfs_label.getMetaphone(), equalTo("SJTRWJNRK"));
        assertThat(rdfs_label.getInLanguage(), equalTo("en-US"));
        assertThat(rdfs_label.getPartition(), equalTo(PartitionKey.lumen_var));
        assertThat(rdfs_label.getConfidence(), equalTo(1f));
        final ThingLabel yago_hasGivenName = labels.stream().filter(it -> "yago:hasGivenName".equals(it.getPropertyQName())).findFirst().get();
        assertThat(yago_hasGivenName.getValue(), equalTo("Sigit Ari"));
        assertThat(yago_hasGivenName.getMetaphone(), equalTo("SJTR"));
        assertThat(yago_hasGivenName.getInLanguage(), equalTo("en-US"));
        assertThat(yago_hasGivenName.getPartition(), equalTo(PartitionKey.lumen_var));
        assertThat(yago_hasGivenName.getConfidence(), equalTo(1f));
        final ThingLabel yago_hasFamilyName = labels.stream().filter(it -> "yago:hasFamilyName".equals(it.getPropertyQName())).findFirst().get();
        assertThat(yago_hasFamilyName.getValue(), equalTo("Wijanarko"));
        assertThat(yago_hasFamilyName.getMetaphone(), equalTo("WJNRK"));
        assertThat(yago_hasFamilyName.getInLanguage(), equalTo("en-US"));
        assertThat(yago_hasFamilyName.getPartition(), equalTo(PartitionKey.lumen_var));
        assertThat(yago_hasFamilyName.getConfidence(), equalTo(1f));
    }

}

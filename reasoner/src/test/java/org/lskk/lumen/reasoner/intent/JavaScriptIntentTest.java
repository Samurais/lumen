package org.lskk.lumen.reasoner.intent;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.LumenCoreConfig;
import org.lskk.lumen.core.RabbitMqConfig;
import org.lskk.lumen.persistence.jpa.YagoTypeRepository;
import org.lskk.lumen.persistence.neo4j.Neo4jConfig;
import org.lskk.lumen.persistence.neo4j.Thing;
import org.lskk.lumen.persistence.neo4j.ThingRepository;
import org.lskk.lumen.persistence.service.FactServiceImpl;
import org.lskk.lumen.reasoner.ux.Fragment;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

import java.util.Locale;

import static org.hamcrest.Matchers.*;

/**
 * Created by ceefour on 18/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = JavaScriptIntentTest.IntentConfig.class)
@SpringApplicationConfiguration(JavaScriptIntentTest.IntentConfig.class)
public class JavaScriptIntentTest {

    @SpringBootApplication(scanBasePackageClasses = {IntentExecutor.class})
    @Import({LumenCoreConfig.class, RabbitMqConfig.class})
//    @Configuration
//    @ComponentScan(basePackageClasses = {IntentExecutor.class, ThingRepository.class, YagoTypeRepository.class, FactServiceImpl.class})
//    @EnableJpaRepositories(basePackageClasses = YagoTypeRepository.class)
//    @EnableNeo4jRepositories(basePackageClasses = ThingRepository.class)
    public static class IntentConfig {

    }

    @Inject
    private IntentExecutor intentExecutor;

    @Test
    public void askBirthDate() {
        final Intent intent = new Intent();
        final InteractionContext interactionContext = new InteractionContext(intent, Locale.forLanguageTag("id-ID"));
        intent.setIntentTypeId("AskBirthDateIntent");
        intent.setConfidence(0.91f);
        intent.setParameters("keyword", "lahir");
        final Thing person = new Thing();
        person.setNn("lumen:Hendy_Irawan");
        person.setPrefLabel("Hendy Irawan");
        person.setPrefLabelLang("id-ID");
        intent.setParameters("person", person);
        intentExecutor.executeIntent(intent, interactionContext);
        assertThat(interactionContext.getReplies(), hasSize(1));
        final String ssml = interactionContext.renderSsml();
        assertThat(ssml, equalTo("Hendy Irawan lahir pada tanggal 14 Desember 1983."));
//        final Fragment response = interactionContext.getReplies().get(0);
    }

//        assertThat(response.get("")interactionContext.getReplies(), hasSize(1));
}

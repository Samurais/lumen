package org.lskk.lumen.reasoner.intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.persistence.jpa.YagoTypeRepository;
import org.lskk.lumen.persistence.neo4j.Neo4jConfig;
import org.lskk.lumen.persistence.neo4j.ThingRepository;
import org.lskk.lumen.persistence.service.FactServiceImpl;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

/**
 * Created by ceefour on 18/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = JavaScriptIntentTest.IntentConfig.class)
@SpringApplicationConfiguration(JavaScriptIntentTest.IntentConfig.class)
public class JavaScriptIntentTest {

    @SpringBootApplication(scanBasePackageClasses = {IntentExecutor.class, ThingRepository.class, YagoTypeRepository.class, FactServiceImpl.class,
            YagoTypeRepository.class, ThingRepository.class
    })
//    @Configuration
//    @ComponentScan(basePackageClasses = {IntentExecutor.class, ThingRepository.class, YagoTypeRepository.class, FactServiceImpl.class})
    @Import(Neo4jConfig.class)
//    @EnableJpaRepositories(basePackageClasses = YagoTypeRepository.class)
//    @EnableNeo4jRepositories(basePackageClasses = ThingRepository.class)
    public static class IntentConfig {

    }

    @Inject
    private IntentExecutor intentExecutor;

    @Test
    public void askBirthDate() {
        final InteractionContext interactionContext = new InteractionContext();
        final Intent intent = new Intent();
        intent.setIntentTypeId("AskBirthDateIntent");
        intent.setConfidence(0.91f);
        intent.setParameters("keyword", "lahir");
        intentExecutor.executeIntent(intent, interactionContext);
    }

}

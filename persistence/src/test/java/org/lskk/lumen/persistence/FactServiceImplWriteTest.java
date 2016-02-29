package org.lskk.lumen.persistence;

import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lskk.lumen.core.LumenCoreConfig;
import org.lskk.lumen.core.LumenProperty;
import org.lskk.lumen.persistence.jpa.YagoTypeRepository;
import org.lskk.lumen.persistence.neo4j.Neo4jConfig;
import org.lskk.lumen.persistence.neo4j.ThingLabel;
import org.lskk.lumen.persistence.neo4j.ThingRepository;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.persistence.service.FactServiceImpl;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

/**
 * uses Mockito: http://www.baeldung.com/injecting-mocks-in-spring
 * Created by ceefour on 18/02/2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = JavaScriptIntentTest.IntentConfig.class)
@SpringApplicationConfiguration(FactServiceImplWriteTest.Config.class)
@ActiveProfiles("spring-data-neo4j")
public class FactServiceImplWriteTest {

    @SpringBootApplication(scanBasePackageClasses = {FactServiceImpl.class, Neo4jConfig.class, ThingRepository.class},
            exclude = {JmxAutoConfiguration.class, CamelAutoConfiguration.class, GroovyTemplateAutoConfiguration.class})
    @Import({LumenCoreConfig.class})
    //@EnableJpaRepositories(basePackageClasses = YagoTypeRepository.class)
//    @EnableNeo4jRepositories(basePackageClasses = ThingRepository.class)
    public static class Config {
    }

    @Inject
    private FactService factService;

    @Test
    public void assertName() {
        final ThingLabel label1 = factService.assertLabel("lumen:Hendy_Irawan", LumenProperty.rdfs_label.getQName(), "Hendy Irawan", "id-ID", new float[]{1f, 1f},
                new DateTime(), null);
        final ThingLabel label2 = factService.assertLabel("lumen:Hendy_Irawan", LumenProperty.skos_prefLabel.getQName(), "Hendy Irawan", "id-ID", new float[]{1f, 1f},
                new DateTime(), null);
        final ThingLabel label3 = factService.assertLabel("lumen:Hendy_Irawan", LumenProperty.yago_isPreferredMeaningOf.getQName(), "Hendy Irawan", "id-ID", new float[]{1f, 0.9f},
                new DateTime(), null);
        final ThingLabel label4 = factService.assertLabel("lumen:Hendy_Irawan", LumenProperty.yago_hasGivenName.getQName(), "Hendy", "id-ID", new float[]{1f, 1f},
                new DateTime(), null);
        final ThingLabel label5 = factService.assertLabel("lumen:Hendy_Irawan", LumenProperty.yago_hasFamilyName.getQName(), "Hendy", "id-ID", new float[]{1f, 1f},
                new DateTime(), null);
    }

}

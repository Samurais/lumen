package org.lskk.lumen.reasoner.aiml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.net.URL;
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
}

package org.lskk.lumen.reasoner;

import org.kie.api.runtime.KieSession;
import org.lskk.lumen.reasoner.event.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.Serializable;

/**
 * Created by ceefour on 14/11/2015.
 */
@Service
public class DroolsService {

    private static final Logger log = LoggerFactory.getLogger(DroolsService.class);

    @Inject
    private KieSession kieSession;

    public void process(AgentResponse resp) {
        for (final Serializable ins : resp.getInsertables()) {
            log.info("Inserting event {}", ins);
            kieSession.insert(ins);
        }
    }
}

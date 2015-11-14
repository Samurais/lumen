package org.lskk.lumen.reasoner.ux;

import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.reasoner.expression.Proposition;
import org.lskk.lumen.reasoner.nlp.NaturalLanguage;
import org.lskk.lumen.reasoner.nlp.en.SentenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Locale;

/**
 * Simply logs using SLF4J.
 * Created by ceefour on 14/11/2015.
 */
@Service
public class LogChannel extends Channel {

    @Override
    public void express(CommunicateAction communicateAction) {
        log.info("Expressing: {}", communicateAction);
    }

}

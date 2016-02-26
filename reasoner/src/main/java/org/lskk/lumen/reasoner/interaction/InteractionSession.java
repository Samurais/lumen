package org.lskk.lumen.reasoner.interaction;

import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.persistence.neo4j.Literal;
import org.lskk.lumen.persistence.neo4j.ThingLabel;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.reasoner.ux.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Soon this will be replaced by {@link org.kie.internal.runtime.StatefulKnowledgeSession},
 * but for now I'll use this to prototype the intended behavior.
 *
 * Serializable, state is stored. So either use Spring to create this prototype bean
 * or pass service beans in methods.
 *
 * Lifecycle:
 * (new) -open-> Running -close-> (gone)
 * Running -poke-> Running
 * Running -suspend-> Suspended
 * Suspended -resume-> Running
 *
 * Created by ceefour on 26/02/2016.
 */
public class InteractionSession implements Serializable, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(InteractionSession.class);
    private List<Locale> activeLocales = new ArrayList<>();
    private InteractionTask activeTask;
    private List<InteractionTask> backgroundTasks = new ArrayList<>();

    @PostConstruct
    public void open() {
        Preconditions.checkState(!activeLocales.isEmpty(),
                "Requires at least one active locale");
    }

    @PreDestroy
    public void close() {

    }

    /**
     * FIXME: find a better name
     */
    public void poke(Channel<?> channel) {
        final Optional<QuestionTemplate> proposition = activeTask.getProposition(activeLocales.get(0));
        if (proposition.isPresent()) {
            final String avatarId = null;
            final CommunicateAction communicateAction = new CommunicateAction(Locale.forLanguageTag(proposition.get().getInLanguage()),
                    proposition.get().getObject(), null);
            channel.express(avatarId, communicateAction, null);
        }
    }

    /**
     * Currently active task will be able to match {@link UtterancePattern}
     * with {@link org.lskk.lumen.reasoner.interaction.UtterancePattern.Scope#LOCAL}.
     * FIXME: this shouldn't be "InteractionTask" behavior but a stateful DTO, i.e. TaskExec
     * @return
     */
    public InteractionTask getActiveTask() {
        return activeTask;
    }

    public void setActiveTask(InteractionTask activeTask) {
        this.activeTask = activeTask;
    }

    /**
     * Background tasks can only match {@link UtterancePattern}
     * with scope {@link org.lskk.lumen.reasoner.interaction.UtterancePattern.Scope#GLOBAL}.
     * FIXME: this shouldn't be "InteractionTask" behavior but a stateful DTO, i.e. TaskExec
     * @return
     */
    public List<InteractionTask> getBackgroundTasks() {
        return backgroundTasks;
    }

    public void setBackgroundTasks(List<InteractionTask> backgroundTasks) {
        this.backgroundTasks = backgroundTasks;
    }

    public List<Locale> getActiveLocales() {
        return activeLocales;
    }

    // TODO: should parameters replaced by CommunicateAction?
    public void receiveUtterance(Locale locale, String text, FactService factService) {
        final CommunicateAction communicateAction = new CommunicateAction(locale, text, null);
        log.info("Executing receiveUtterance for {}: {}", activeTask, communicateAction);
        activeTask.receiveUtterance(communicateAction);
        while (!activeTask.getLabelsToAssert().isEmpty()) {
            final ThingLabel label = activeTask.getLabelsToAssert().poll();
            log.info("Asserting {}", label);
            factService.assertLabel(label.getThingQName(), label.getPropertyQName(),
                    label.getValue(), label.getInLanguage(),
                    new float[] {1f, label.getConfidence(), 0}, new DateTime(), null);
        }
        while (!activeTask.getLiteralsToAssert().isEmpty()) {
            final Literal literal = activeTask.getLiteralsToAssert().poll();
            log.info("Asserting {}", literal);
            factService.assertPropertyToLiteral(literal.getSubject().getNn(), literal.getPredicate().getNn(),
                    literal.getType(), literal.getValue(),
                    new float[] {1f, literal.getConfidence(), 0}, new DateTime(), null);
        }
    }
}

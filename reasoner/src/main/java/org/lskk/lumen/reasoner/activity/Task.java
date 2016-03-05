package org.lskk.lumen.reasoner.activity;

import org.joda.time.DateTime;
import org.lskk.lumen.core.CommunicateAction;
import org.lskk.lumen.persistence.neo4j.Literal;
import org.lskk.lumen.persistence.neo4j.ThingLabel;
import org.lskk.lumen.reasoner.expression.Proposition;
import org.lskk.lumen.reasoner.intent.Slot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Created by ceefour on 04/03/2016.
 */
public abstract class Task extends Activity {
    private List<UtterancePattern> matchedUtterancePatterns = new ArrayList<>();
    private Queue<ThingLabel> labelsToAssert = new ArrayDeque<>();
    private Queue<Literal> literalsToAssert = new ArrayDeque<>();
    private Queue<Proposition> pendingPropositions = new ArrayDeque<>();

    /**
     * Used by {@link PromptTask}.
     * @return
     */
    public List<UtterancePattern> getMatchedUtterancePatterns() {
        return matchedUtterancePatterns;
    }

    /**
     * Assertable {@link ThingLabel}s, they will be {@link Queue#poll()}-ed by {@link InteractionSession}
     * and asserted to Persistence using {@link org.lskk.lumen.persistence.service.FactService#assertLabel(String, String, String, String, float[], DateTime, String)}.
     * @return
     */
    public Queue<ThingLabel> getLabelsToAssert() {
        return labelsToAssert;
    }

    /**
     * Assertable {@link Literal}s, they will be {@link Queue#poll()}-ed by {@link InteractionSession}
     * and asserted to Persistence using {@link org.lskk.lumen.persistence.service.FactService#assertPropertyToLiteral(String, String, String, Object, float[], DateTime, String)}.
     * @return
     */
    public Queue<Literal> getLiteralsToAssert() {
        return literalsToAssert;
    }

    /**
     * Will be {@link Queue#poll()}-ed by {@link InteractionSession} to {@link org.lskk.lumen.reasoner.ux.Channel#express(String, Proposition, boolean, Object)}
     * back to the user.
     * @return
     */
    public Queue<Proposition> getPendingPropositions() {
        return pendingPropositions;
    }

}

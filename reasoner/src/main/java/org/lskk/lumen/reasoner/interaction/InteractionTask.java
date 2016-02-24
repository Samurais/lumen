package org.lskk.lumen.reasoner.interaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Base class for natural interaction pattern that is readily usable.
 * An task instance only specifies behavior, does not save state, so can be reused by different processes.
 *
 * Created by ceefour on 17/02/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class InteractionTask {
}

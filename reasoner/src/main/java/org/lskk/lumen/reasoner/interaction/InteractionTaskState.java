package org.lskk.lumen.reasoner.interaction;

import org.kie.api.runtime.process.ProcessInstance;

/**
 * Inspired by {@link ProcessInstance#getState()}.
 * Created by ceefour on 28/02/2016.
 */
public enum InteractionTaskState {
    PENDING,
    ACTIVE,
    COMPLETED,
    ABORTED,
    SUSPENDED,
}

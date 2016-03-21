package org.lskk.lumen.reasoner.activity;

import org.kie.api.runtime.process.ProcessInstance;

/**
 * Inspired by {@link ProcessInstance#getState()}.
 * Created by ceefour on 28/02/2016.
 */
public enum ActivityState {
    PENDING,
    ACTIVE,
    COMPLETED,
    ABORTED,
    SUSPENDED,
}

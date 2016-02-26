package org.lskk.lumen.persistence.neo4j;

/**
 * Created by ceefour on 15/02/2016.
 */
public enum PartitionKey {
    /**
     * Unchanged, read-only YAGO nodes and links.
     * Only changed when we update from YAGO.
     */
    lumen_yago,
    /**
     * Common Knowledge Base for all Lumen deployments.
     * Changed during development of Lumen, but not changed during "normal" execution.
     */
    lumen_common,
    /**
     * Variable, deployment-specific Knowledge Base that constantly changes.
     */
    lumen_var
}

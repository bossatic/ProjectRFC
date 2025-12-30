package org.dataingest.rfc.server.monitoring.events;

/**
 * Stages where errors can occur
 */
public enum ErrorStage {
    XML_WRITE,
    JSON_CONVERSION,
    KAFKA_PUBLISH,
    SYSTEM
}

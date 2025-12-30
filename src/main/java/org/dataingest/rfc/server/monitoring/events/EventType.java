package org.dataingest.rfc.server.monitoring.events;

/**
 * Types of monitoring events
 */
public enum EventType {
    IDOC_RECEIVED,
    IDOC_PROCESSED,
    KAFKA_PUBLISHED,
    ERROR
}

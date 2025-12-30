package org.dataingest.rfc.server.monitoring.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all monitoring events
 */
public abstract class MonitoringEvent {
    private final String eventId;
    private final Instant timestamp;
    private final EventType eventType;

    public MonitoringEvent(EventType eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public EventType getEventType() {
        return eventType;
    }

    /**
     * Convert event to JSON string
     */
    public abstract String toJson();

    /**
     * Get document number for Kafka key (null if not applicable)
     */
    public abstract String getDocNum();
}

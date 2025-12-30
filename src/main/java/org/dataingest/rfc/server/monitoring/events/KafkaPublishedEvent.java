package org.dataingest.rfc.server.monitoring.events;

/**
 * Event fired when an IDoc is published to Kafka
 */
public class KafkaPublishedEvent extends MonitoringEvent {
    private final String idocType;
    private final String docNum;
    private final String topic;
    private final int partition;
    private final long offset;
    private final long latencyMs;

    public KafkaPublishedEvent(String idocType, String docNum, String topic,
                               int partition, long offset, long latencyMs) {
        super(EventType.KAFKA_PUBLISHED);
        this.idocType = idocType;
        this.docNum = docNum;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.latencyMs = latencyMs;
    }

    public String getIdocType() {
        return idocType;
    }

    @Override
    public String getDocNum() {
        return docNum;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    @Override
    public String toJson() {
        return String.format(
            "{\"event_id\":\"%s\",\"timestamp\":\"%s\",\"event_type\":\"KAFKA_PUBLISHED\"," +
            "\"payload\":{\"idoc_type\":\"%s\",\"doc_num\":\"%s\",\"topic\":\"%s\"," +
            "\"partition\":%d,\"offset\":%d,\"latency_ms\":%d}}",
            getEventId(), getTimestamp(), idocType, docNum, topic, partition, offset, latencyMs
        );
    }
}

package org.dataingest.rfc.server.monitoring.events;

/**
 * Event fired when an IDoc is received
 */
public class IdocReceivedEvent extends MonitoringEvent {
    private final String idocType;
    private final String docNum;
    private final String tid;
    private final long sizeBytes;
    private final String sourceSystem;

    public IdocReceivedEvent(String idocType, String docNum, String tid, long sizeBytes, String sourceSystem) {
        super(EventType.IDOC_RECEIVED);
        this.idocType = idocType;
        this.docNum = docNum;
        this.tid = tid;
        this.sizeBytes = sizeBytes;
        this.sourceSystem = sourceSystem;
    }

    public String getIdocType() {
        return idocType;
    }

    @Override
    public String getDocNum() {
        return docNum;
    }

    public String getTid() {
        return tid;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    @Override
    public String toJson() {
        return String.format(
            "{\"event_id\":\"%s\",\"timestamp\":\"%s\",\"event_type\":\"IDOC_RECEIVED\"," +
            "\"payload\":{\"idoc_type\":\"%s\",\"doc_num\":\"%s\",\"tid\":\"%s\"," +
            "\"size_bytes\":%d,\"source_system\":\"%s\"}}",
            getEventId(), getTimestamp(), idocType, docNum, tid, sizeBytes,
            sourceSystem != null ? sourceSystem : ""
        );
    }
}

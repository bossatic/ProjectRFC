package org.dataingest.rfc.server.monitoring.events;

import java.util.List;

/**
 * Event fired when an IDoc is processed (XML written, JSON converted)
 */
public class IdocProcessedEvent extends MonitoringEvent {
    private final String idocType;
    private final String docNum;
    private final long processingTimeMs;
    private final long xmlSizeBytes;
    private final long jsonSizeBytes;
    private final List<String> stagesCompleted;

    public IdocProcessedEvent(String idocType, String docNum, long processingTimeMs,
                             long xmlSizeBytes, long jsonSizeBytes, List<String> stagesCompleted) {
        super(EventType.IDOC_PROCESSED);
        this.idocType = idocType;
        this.docNum = docNum;
        this.processingTimeMs = processingTimeMs;
        this.xmlSizeBytes = xmlSizeBytes;
        this.jsonSizeBytes = jsonSizeBytes;
        this.stagesCompleted = stagesCompleted;
    }

    public String getIdocType() {
        return idocType;
    }

    @Override
    public String getDocNum() {
        return docNum;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public long getXmlSizeBytes() {
        return xmlSizeBytes;
    }

    public long getJsonSizeBytes() {
        return jsonSizeBytes;
    }

    public List<String> getStagesCompleted() {
        return stagesCompleted;
    }

    @Override
    public String toJson() {
        StringBuilder stages = new StringBuilder("[");
        for (int i = 0; i < stagesCompleted.size(); i++) {
            stages.append("\"").append(stagesCompleted.get(i)).append("\"");
            if (i < stagesCompleted.size() - 1) stages.append(",");
        }
        stages.append("]");

        return String.format(
            "{\"event_id\":\"%s\",\"timestamp\":\"%s\",\"event_type\":\"IDOC_PROCESSED\"," +
            "\"payload\":{\"idoc_type\":\"%s\",\"doc_num\":\"%s\",\"processing_time_ms\":%d," +
            "\"xml_size_bytes\":%d,\"json_size_bytes\":%d,\"stages_completed\":%s}}",
            getEventId(), getTimestamp(), idocType, docNum, processingTimeMs,
            xmlSizeBytes, jsonSizeBytes, stages.toString()
        );
    }
}

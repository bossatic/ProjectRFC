package org.dataingest.rfc.server.monitoring;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable snapshot of current metrics
 */
public class MetricsSnapshot {
    private final long totalIdocsReceived;
    private final Map<String, Long> idocsByType;
    private final long totalXmlWritten;
    private final long totalJsonConverted;
    private final long totalKafkaPublished;
    private final long xmlErrors;
    private final long jsonErrors;
    private final long kafkaErrors;
    private final int currentlyProcessing;
    private final Instant lastIdocReceived;
    private final String lastIdocType;
    private final String lastDocNum;
    private final double receptionRatePer5Min;
    private final double receptionRatePer1Hour;
    private final MetricsStore.KafkaConnectionStatus kafkaStatus;
    private final Instant applicationStartTime;

    public MetricsSnapshot(long totalIdocsReceived, Map<String, Long> idocsByType,
                          long totalXmlWritten, long totalJsonConverted, long totalKafkaPublished,
                          long xmlErrors, long jsonErrors, long kafkaErrors,
                          int currentlyProcessing, Instant lastIdocReceived,
                          String lastIdocType, String lastDocNum,
                          double receptionRatePer5Min, double receptionRatePer1Hour,
                          MetricsStore.KafkaConnectionStatus kafkaStatus, Instant applicationStartTime) {
        this.totalIdocsReceived = totalIdocsReceived;
        this.idocsByType = idocsByType;
        this.totalXmlWritten = totalXmlWritten;
        this.totalJsonConverted = totalJsonConverted;
        this.totalKafkaPublished = totalKafkaPublished;
        this.xmlErrors = xmlErrors;
        this.jsonErrors = jsonErrors;
        this.kafkaErrors = kafkaErrors;
        this.currentlyProcessing = currentlyProcessing;
        this.lastIdocReceived = lastIdocReceived;
        this.lastIdocType = lastIdocType;
        this.lastDocNum = lastDocNum;
        this.receptionRatePer5Min = receptionRatePer5Min;
        this.receptionRatePer1Hour = receptionRatePer1Hour;
        this.kafkaStatus = kafkaStatus;
        this.applicationStartTime = applicationStartTime;
    }

    // Getters
    public long getTotalIdocsReceived() { return totalIdocsReceived; }
    public Map<String, Long> getIdocsByType() { return idocsByType; }
    public long getTotalXmlWritten() { return totalXmlWritten; }
    public long getTotalJsonConverted() { return totalJsonConverted; }
    public long getTotalKafkaPublished() { return totalKafkaPublished; }
    public long getXmlErrors() { return xmlErrors; }
    public long getJsonErrors() { return jsonErrors; }
    public long getKafkaErrors() { return kafkaErrors; }
    public int getCurrentlyProcessing() { return currentlyProcessing; }
    public Instant getLastIdocReceived() { return lastIdocReceived; }
    public String getLastIdocType() { return lastIdocType; }
    public String getLastDocNum() { return lastDocNum; }
    public double getReceptionRatePer5Min() { return receptionRatePer5Min; }
    public double getReceptionRatePer1Hour() { return receptionRatePer1Hour; }
    public MetricsStore.KafkaConnectionStatus getKafkaStatus() { return kafkaStatus; }
    public Instant getApplicationStartTime() { return applicationStartTime; }

    public long getTotalErrors() {
        return xmlErrors + jsonErrors + kafkaErrors;
    }

    public long getTotalSuccess() {
        return totalIdocsReceived - getTotalErrors();
    }

    public double getSuccessRate() {
        if (totalIdocsReceived == 0) return 100.0;
        return (getTotalSuccess() / (double) totalIdocsReceived) * 100.0;
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("{");
        json.append("\"totalIdocsReceived\":").append(totalIdocsReceived).append(",");
        json.append("\"idocsByType\":{");
        int i = 0;
        for (Map.Entry<String, Long> entry : idocsByType.entrySet()) {
            if (i > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            i++;
        }
        json.append("},");
        json.append("\"totalXmlWritten\":").append(totalXmlWritten).append(",");
        json.append("\"totalJsonConverted\":").append(totalJsonConverted).append(",");
        json.append("\"totalKafkaPublished\":").append(totalKafkaPublished).append(",");
        json.append("\"errors\":{");
        json.append("\"xml\":").append(xmlErrors).append(",");
        json.append("\"json\":").append(jsonErrors).append(",");
        json.append("\"kafka\":").append(kafkaErrors);
        json.append("},");
        json.append("\"currentlyProcessing\":").append(currentlyProcessing).append(",");
        json.append("\"lastIdocReceived\":\"").append(lastIdocReceived != null ? lastIdocReceived : "").append("\",");
        json.append("\"lastIdocType\":\"").append(lastIdocType != null ? lastIdocType : "").append("\",");
        json.append("\"lastDocNum\":\"").append(lastDocNum != null ? lastDocNum : "").append("\",");
        json.append("\"receptionRatePer5Min\":").append(String.format("%.2f", receptionRatePer5Min)).append(",");
        json.append("\"receptionRatePer1Hour\":").append(String.format("%.2f", receptionRatePer1Hour)).append(",");
        json.append("\"kafkaStatus\":\"").append(kafkaStatus).append("\",");
        json.append("\"applicationStartTime\":\"").append(applicationStartTime).append("\",");
        json.append("\"successRate\":").append(String.format("%.2f", getSuccessRate()));
        json.append("}");
        return json.toString();
    }
}

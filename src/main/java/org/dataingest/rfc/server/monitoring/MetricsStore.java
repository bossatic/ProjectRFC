package org.dataingest.rfc.server.monitoring;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;

import org.dataingest.rfc.server.monitoring.util.CircularBuffer;
import org.dataingest.rfc.server.monitoring.events.ErrorStage;

/**
 * In-memory metrics store for real-time dashboard
 */
public class MetricsStore {

    // Counters
    private final AtomicLong totalIdocsReceived = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> idocsByType = new ConcurrentHashMap<>();
    private final AtomicLong totalXmlWritten = new AtomicLong(0);
    private final AtomicLong totalJsonConverted = new AtomicLong(0);
    private final AtomicLong totalKafkaPublished = new AtomicLong(0);

    // Errors
    private final AtomicLong xmlErrors = new AtomicLong(0);
    private final AtomicLong jsonErrors = new AtomicLong(0);
    private final AtomicLong kafkaErrors = new AtomicLong(0);

    // Gauges
    private final AtomicInteger currentlyProcessing = new AtomicInteger(0);
    private volatile Instant lastIdocReceived;
    private volatile String lastIdocType;
    private volatile String lastDocNum;

    // Time windows for rate calculation
    private final CircularBuffer<TimestampedEvent> last5Minutes = new CircularBuffer<>(300);
    private final CircularBuffer<TimestampedEvent> last1Hour = new CircularBuffer<>(3600);

    // System
    private final Instant applicationStartTime = Instant.now();
    private volatile KafkaConnectionStatus kafkaStatus = KafkaConnectionStatus.UNKNOWN;

    // Increment operations
    public void incrementIdocsReceived(String idocType) {
        totalIdocsReceived.incrementAndGet();
        idocsByType.computeIfAbsent(idocType, k -> new AtomicLong(0)).incrementAndGet();

        // Add to time windows
        TimestampedEvent event = new TimestampedEvent(Instant.now(), idocType);
        last5Minutes.add(event);
        last1Hour.add(event);
    }

    public void incrementXmlWritten() {
        totalXmlWritten.incrementAndGet();
    }

    public void incrementJsonConverted() {
        totalJsonConverted.incrementAndGet();
    }

    public void incrementKafkaPublished() {
        totalKafkaPublished.incrementAndGet();
    }

    public void incrementXmlErrors() {
        xmlErrors.incrementAndGet();
    }

    public void incrementJsonErrors() {
        jsonErrors.incrementAndGet();
    }

    public void incrementKafkaErrors() {
        kafkaErrors.incrementAndGet();
    }

    /**
     * Increment error counter based on error stage
     */
    public void incrementError(ErrorStage stage) {
        switch (stage) {
            case XML_WRITE:
                incrementXmlErrors();
                break;
            case JSON_CONVERSION:
                incrementJsonErrors();
                break;
            case KAFKA_PUBLISH:
                incrementKafkaErrors();
                break;
            case SYSTEM:
                // Could add a system error counter if needed
                break;
        }
    }

    public void incrementCurrentlyProcessing() {
        currentlyProcessing.incrementAndGet();
    }

    public void decrementCurrentlyProcessing() {
        currentlyProcessing.decrementAndGet();
    }

    // Update operations
    public void updateLastReceived(String idocType, String docNum, Instant timestamp) {
        this.lastIdocReceived = timestamp;
        this.lastIdocType = idocType;
        this.lastDocNum = docNum;
    }

    public void updateKafkaStatus(KafkaConnectionStatus status) {
        this.kafkaStatus = status;
    }

    // Query operations
    public long getTotalIdocsReceived() {
        return totalIdocsReceived.get();
    }

    public Map<String, Long> getIdocsByType() {
        Map<String, Long> result = new HashMap<>();
        idocsByType.forEach((key, value) -> result.put(key, value.get()));
        return result;
    }

    public long getTotalXmlWritten() {
        return totalXmlWritten.get();
    }

    public long getTotalJsonConverted() {
        return totalJsonConverted.get();
    }

    public long getTotalKafkaPublished() {
        return totalKafkaPublished.get();
    }

    public long getXmlErrors() {
        return xmlErrors.get();
    }

    public long getJsonErrors() {
        return jsonErrors.get();
    }

    public long getKafkaErrors() {
        return kafkaErrors.get();
    }

    public int getCurrentlyProcessing() {
        return currentlyProcessing.get();
    }

    public Instant getLastIdocReceived() {
        return lastIdocReceived;
    }

    public String getLastIdocType() {
        return lastIdocType;
    }

    public String getLastDocNum() {
        return lastDocNum;
    }

    public KafkaConnectionStatus getKafkaStatus() {
        return kafkaStatus;
    }

    public Instant getApplicationStartTime() {
        return applicationStartTime;
    }

    /**
     * Calculate reception rate in IDocs per minute
     */
    public double getReceptionRate(TimeWindow window) {
        Instant now = Instant.now();
        Instant cutoff;
        CircularBuffer<TimestampedEvent> buffer;

        switch (window) {
            case LAST_5_MINUTES:
                cutoff = now.minusSeconds(300);
                buffer = last5Minutes;
                break;
            case LAST_1_HOUR:
                cutoff = now.minusSeconds(3600);
                buffer = last1Hour;
                break;
            default:
                return 0.0;
        }

        // Count events within time window
        int count = 0;
        Object[] entries = buffer.getAllEntries();
        for (Object entry : entries) {
            if (entry != null) {
                TimestampedEvent event = (TimestampedEvent) entry;
                if (event.timestamp.isAfter(cutoff)) {
                    count++;
                }
            }
        }

        // Calculate rate per minute
        long secondsInWindow = window == TimeWindow.LAST_5_MINUTES ? 300 : 3600;
        return (count / (double) secondsInWindow) * 60.0;
    }

    /**
     * Get complete metrics snapshot
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            totalIdocsReceived.get(),
            new HashMap<>(getIdocsByType()),
            totalXmlWritten.get(),
            totalJsonConverted.get(),
            totalKafkaPublished.get(),
            xmlErrors.get(),
            jsonErrors.get(),
            kafkaErrors.get(),
            currentlyProcessing.get(),
            lastIdocReceived,
            lastIdocType,
            lastDocNum,
            getReceptionRate(TimeWindow.LAST_5_MINUTES),
            getReceptionRate(TimeWindow.LAST_1_HOUR),
            kafkaStatus,
            applicationStartTime
        );
    }

    /**
     * Initialize metrics from database (called on startup)
     * Loads historical totals to display on dashboard after restart
     */
    public void initializeFromDatabase(DataSource dataSource) {
        if (dataSource == null) {
            return;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Get overall totals from events table
            String totalsSql = "SELECT " +
                "COUNT(*) as total_received, " +
                "COUNT(CASE WHEN stage = 'XML_WRITE' AND status = 'SUCCESS' THEN 1 END) as total_xml, " +
                "COUNT(CASE WHEN stage = 'PROCESSED' AND status = 'SUCCESS' THEN 1 END) as total_json, " +
                "COUNT(CASE WHEN stage = 'KAFKA_PUBLISH' AND status = 'SUCCESS' THEN 1 END) as total_kafka, " +
                "COUNT(CASE WHEN event_type = 'ERROR' AND stage = 'XML_WRITE' THEN 1 END) as xml_errors, " +
                "COUNT(CASE WHEN event_type = 'ERROR' AND stage = 'JSON_CONVERSION' THEN 1 END) as json_errors, " +
                "COUNT(CASE WHEN event_type = 'ERROR' AND stage = 'KAFKA_PUBLISH' THEN 1 END) as kafka_errors " +
                "FROM idoc_events";

            ResultSet rs = stmt.executeQuery(totalsSql);
            if (rs.next()) {
                totalIdocsReceived.set(rs.getLong("total_received"));
                totalXmlWritten.set(rs.getLong("total_xml"));
                totalJsonConverted.set(rs.getLong("total_json"));
                totalKafkaPublished.set(rs.getLong("total_kafka"));
                xmlErrors.set(rs.getLong("xml_errors"));
                jsonErrors.set(rs.getLong("json_errors"));
                kafkaErrors.set(rs.getLong("kafka_errors"));
            }
            rs.close();

            // Get counts by IDoc type
            String typesSql = "SELECT idoc_type, COUNT(*) as count " +
                "FROM idoc_events " +
                "WHERE idoc_type IS NOT NULL " +
                "GROUP BY idoc_type";

            ResultSet typesRs = stmt.executeQuery(typesSql);
            while (typesRs.next()) {
                String idocType = typesRs.getString("idoc_type");
                long count = typesRs.getLong("count");
                idocsByType.put(idocType, new AtomicLong(count));
            }
            typesRs.close();

            // Get last received IDoc info
            String lastSql = "SELECT idoc_type, doc_num, timestamp " +
                "FROM idoc_events " +
                "WHERE event_type = 'IDOC_RECEIVED' " +
                "ORDER BY timestamp DESC " +
                "LIMIT 1";

            ResultSet lastRs = stmt.executeQuery(lastSql);
            if (lastRs.next()) {
                lastIdocType = lastRs.getString("idoc_type");
                lastDocNum = lastRs.getString("doc_num");
                lastIdocReceived = lastRs.getTimestamp("timestamp").toInstant();
            }
            lastRs.close();

        } catch (Exception e) {
            // Log but don't fail - metrics will start fresh
            System.err.println("[MetricsStore] Failed to load historical metrics from database: " + e.getMessage());
        }
    }

    // Inner classes
    public static class TimestampedEvent {
        final Instant timestamp;
        final String idocType;

        public TimestampedEvent(Instant timestamp, String idocType) {
            this.timestamp = timestamp;
            this.idocType = idocType;
        }
    }

    public enum TimeWindow {
        LAST_5_MINUTES,
        LAST_1_HOUR
    }

    public enum KafkaConnectionStatus {
        CONNECTED,
        DISCONNECTED,
        UNKNOWN
    }
}

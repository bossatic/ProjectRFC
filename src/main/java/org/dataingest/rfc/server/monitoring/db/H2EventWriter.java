package org.dataingest.rfc.server.monitoring.db;

import org.dataingest.rfc.server.config.IDocCaptureConfig;
import org.dataingest.rfc.server.monitoring.events.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Writes monitoring events to H2 database in batches
 */
public class H2EventWriter {
    private final DataSource dataSource;
    private final IDocCaptureConfig config;

    public H2EventWriter(DataSource dataSource, IDocCaptureConfig config) {
        this.dataSource = dataSource;
        this.config = config;
    }

    public void batchInsert(List<MonitoringEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO idoc_events (" +
            "event_id, timestamp, event_type, idoc_type, doc_num, " +
            "tid, stage, status, processing_time_ms, " +
            "xml_size_bytes, json_size_bytes, " +
            "kafka_topic, kafka_partition, kafka_offset, " +
            "error_message, error_stage, is_recoverable, " +
            "source_system, payload_json" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (MonitoringEvent event : events) {
                setParameters(stmt, event);
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            conn.commit();

            config.log("[Monitoring] Batch inserted " + results.length + " events to H2");

        } catch (SQLException e) {
            config.logError("[Monitoring] Failed to write batch to H2", e);
        }
    }

    private void setParameters(PreparedStatement stmt, MonitoringEvent event) throws SQLException {
        stmt.setString(1, event.getEventId());
        stmt.setTimestamp(2, Timestamp.from(event.getTimestamp()));
        stmt.setString(3, event.getEventType().name());

        // Reset all optional fields
        for (int i = 4; i <= 19; i++) {
            stmt.setNull(i, java.sql.Types.VARCHAR);
        }

        // Type-specific fields
        if (event instanceof IdocReceivedEvent) {
            IdocReceivedEvent e = (IdocReceivedEvent) event;
            stmt.setString(4, e.getIdocType());
            stmt.setString(5, e.getDocNum());
            stmt.setString(6, e.getTid());
            stmt.setString(7, "RECEIVED");
            stmt.setString(8, "SUCCESS");
            stmt.setLong(10, e.getSizeBytes());
            stmt.setString(18, e.getSourceSystem());

        } else if (event instanceof IdocProcessedEvent) {
            IdocProcessedEvent e = (IdocProcessedEvent) event;
            stmt.setString(4, e.getIdocType());
            stmt.setString(5, e.getDocNum());
            stmt.setString(7, "PROCESSED");
            stmt.setString(8, "SUCCESS");
            stmt.setInt(9, (int) e.getProcessingTimeMs());
            stmt.setLong(10, e.getXmlSizeBytes());
            stmt.setLong(11, e.getJsonSizeBytes());

        } else if (event instanceof KafkaPublishedEvent) {
            KafkaPublishedEvent e = (KafkaPublishedEvent) event;
            stmt.setString(4, e.getIdocType());
            stmt.setString(5, e.getDocNum());
            stmt.setString(7, "KAFKA_PUBLISH");
            stmt.setString(8, "SUCCESS");
            stmt.setString(12, e.getTopic());
            stmt.setInt(13, e.getPartition());
            stmt.setLong(14, e.getOffset());

        } else if (event instanceof ErrorEvent) {
            ErrorEvent e = (ErrorEvent) event;
            stmt.setString(4, e.getIdocType());
            stmt.setString(5, e.getDocNum());
            stmt.setString(7, e.getStage().name());
            stmt.setString(8, "FAILED");
            stmt.setString(15, e.getErrorMessage());
            stmt.setString(16, e.getStage().name());
            stmt.setBoolean(17, e.isRecoverable());
        }

        // Always set payload JSON
        stmt.setString(19, event.toJson());
    }
}

package org.dataingest.rfc.server.monitoring.db;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Query service for retrieving historical data from H2
 */
public class H2QueryService {
    private final DataSource dataSource;

    public H2QueryService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<IdocEventDto> getRecentEvents(int limit) {
        String sql = "SELECT event_id, timestamp, event_type, idoc_type, " +
                    "doc_num, status, processing_time_ms " +
                    "FROM idoc_events " +
                    "WHERE timestamp > DATEADD('HOUR', -1, CURRENT_TIMESTAMP) " +
                    "ORDER BY timestamp DESC " +
                    "LIMIT ?";

        List<IdocEventDto> events = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(new IdocEventDto(
                        rs.getString("event_id"),
                        rs.getTimestamp("timestamp").toInstant(),
                        rs.getString("event_type"),
                        rs.getString("idoc_type"),
                        rs.getString("doc_num"),
                        rs.getString("status"),
                        rs.getInt("processing_time_ms")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    /**
     * Query events with flexible filtering
     */
    public List<IdocEventDto> queryEventsFiltered(
            Instant startTime,
            Instant endTime,
            String idocType,
            String eventType,
            String status,
            int limit) {

        StringBuilder sql = new StringBuilder(
            "SELECT event_id, timestamp, event_type, idoc_type, " +
            "doc_num, status, processing_time_ms " +
            "FROM idoc_events WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        // Add time range filter
        if (startTime != null) {
            sql.append(" AND timestamp >= ?");
            params.add(Timestamp.from(startTime));
        }
        if (endTime != null) {
            sql.append(" AND timestamp <= ?");
            params.add(Timestamp.from(endTime));
        }

        // Add IDoc type filter
        if (idocType != null && !idocType.isEmpty() && !idocType.equals("all")) {
            sql.append(" AND idoc_type = ?");
            params.add(idocType);
        }

        // Add event type filter
        if (eventType != null && !eventType.isEmpty() && !eventType.equals("all")) {
            sql.append(" AND event_type = ?");
            params.add(eventType);
        }

        // Add status filter
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        sql.append(" ORDER BY timestamp DESC LIMIT ?");
        params.add(limit);

        List<IdocEventDto> events = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            // Set parameters
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Timestamp) {
                    stmt.setTimestamp(i + 1, (Timestamp) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else {
                    stmt.setString(i + 1, param.toString());
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(new IdocEventDto(
                        rs.getString("event_id"),
                        rs.getTimestamp("timestamp").toInstant(),
                        rs.getString("event_type"),
                        rs.getString("idoc_type"),
                        rs.getString("doc_num"),
                        rs.getString("status"),
                        rs.getInt("processing_time_ms")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    /**
     * Get unique IDoc types from database
     */
    public List<String> getUniqueIdocTypes() {
        String sql = "SELECT DISTINCT idoc_type FROM idoc_events " +
                    "WHERE idoc_type IS NOT NULL " +
                    "ORDER BY idoc_type";

        List<String> types = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                types.add(rs.getString("idoc_type"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return types;
    }

    public List<IdocEventDto> queryByDateRange(Instant start, Instant end, String idocType, int limit) {
        StringBuilder sql = new StringBuilder(
            "SELECT event_id, timestamp, event_type, idoc_type, " +
            "doc_num, status, processing_time_ms " +
            "FROM idoc_events WHERE timestamp BETWEEN ? AND ?"
        );

        if (idocType != null && !idocType.isEmpty()) {
            sql.append(" AND idoc_type = ?");
        }

        sql.append(" ORDER BY timestamp DESC LIMIT ?");

        List<IdocEventDto> events = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setTimestamp(1, Timestamp.from(start));
            stmt.setTimestamp(2, Timestamp.from(end));

            int paramIndex = 3;
            if (idocType != null && !idocType.isEmpty()) {
                stmt.setString(paramIndex++, idocType);
            }
            stmt.setInt(paramIndex, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(new IdocEventDto(
                        rs.getString("event_id"),
                        rs.getTimestamp("timestamp").toInstant(),
                        rs.getString("event_type"),
                        rs.getString("idoc_type"),
                        rs.getString("doc_num"),
                        rs.getString("status"),
                        rs.getInt("processing_time_ms")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    public List<ErrorEventDto> getErrors(Instant since, int limit) {
        String sql = "SELECT event_id, timestamp, idoc_type, doc_num, " +
                    "error_stage, error_message, is_recoverable " +
                    "FROM idoc_events " +
                    "WHERE event_type = 'ERROR' AND timestamp > ? " +
                    "ORDER BY timestamp DESC LIMIT ?";

        List<ErrorEventDto> errors = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.from(since));
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    errors.add(new ErrorEventDto(
                        rs.getString("event_id"),
                        rs.getTimestamp("timestamp").toInstant(),
                        rs.getString("idoc_type"),
                        rs.getString("doc_num"),
                        rs.getString("error_stage"),
                        rs.getString("error_message"),
                        rs.getBoolean("is_recoverable")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return errors;
    }

    public DailySummaryDto getDailySummary(LocalDate date) {
        String sql = "SELECT " +
                    "COUNT(*) as total, " +
                    "COUNT(CASE WHEN status = 'SUCCESS' THEN 1 END) as success, " +
                    "COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed, " +
                    "AVG(processing_time_ms) as avg_time, " +
                    "SUM(xml_size_bytes) as total_xml_bytes, " +
                    "SUM(json_size_bytes) as total_json_bytes " +
                    "FROM idoc_events " +
                    "WHERE CAST(timestamp AS DATE) = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new DailySummaryDto(
                        date,
                        rs.getLong("total"),
                        rs.getLong("success"),
                        rs.getLong("failed"),
                        rs.getLong("avg_time"),
                        rs.getLong("total_xml_bytes"),
                        rs.getLong("total_json_bytes")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // DTOs
    public static class IdocEventDto {
        public final String eventId;
        public final Instant timestamp;
        public final String eventType;
        public final String idocType;
        public final String docNum;
        public final String status;
        public final int processingTimeMs;

        public IdocEventDto(String eventId, Instant timestamp, String eventType,
                           String idocType, String docNum, String status, int processingTimeMs) {
            this.eventId = eventId;
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.idocType = idocType;
            this.docNum = docNum;
            this.status = status;
            this.processingTimeMs = processingTimeMs;
        }
    }

    public static class ErrorEventDto {
        public final String eventId;
        public final Instant timestamp;
        public final String idocType;
        public final String docNum;
        public final String errorStage;
        public final String errorMessage;
        public final boolean recoverable;

        public ErrorEventDto(String eventId, Instant timestamp, String idocType,
                            String docNum, String errorStage, String errorMessage, boolean recoverable) {
            this.eventId = eventId;
            this.timestamp = timestamp;
            this.idocType = idocType;
            this.docNum = docNum;
            this.errorStage = errorStage;
            this.errorMessage = errorMessage;
            this.recoverable = recoverable;
        }
    }

    public static class DailySummaryDto {
        public final LocalDate date;
        public final long total;
        public final long success;
        public final long failed;
        public final long avgProcessingTimeMs;
        public final long totalXmlBytes;
        public final long totalJsonBytes;

        public DailySummaryDto(LocalDate date, long total, long success, long failed,
                              long avgProcessingTimeMs, long totalXmlBytes, long totalJsonBytes) {
            this.date = date;
            this.total = total;
            this.success = success;
            this.failed = failed;
            this.avgProcessingTimeMs = avgProcessingTimeMs;
            this.totalXmlBytes = totalXmlBytes;
            this.totalJsonBytes = totalJsonBytes;
        }
    }
}

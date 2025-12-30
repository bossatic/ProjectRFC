package org.dataingest.rfc.server.monitoring.db;

import org.dataingest.rfc.server.config.IDocCaptureConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background job to aggregate hourly statistics
 */
public class HourlyAggregationJob {
    private final DataSource dataSource;
    private final IDocCaptureConfig config;
    private final ScheduledExecutorService scheduler;

    public HourlyAggregationJob(DataSource dataSource, IDocCaptureConfig config) {
        this.dataSource = dataSource;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        // Run initial aggregation immediately for existing data
        config.log("[Monitoring] Running initial hourly aggregation...");
        aggregateAllHistory();

        long initialDelay = getDelayUntilNextHour() + 300; // +5 minutes past the hour
        long period = 60 * 60; // 1 hour in seconds

        scheduler.scheduleAtFixedRate(
            this::aggregate,
            initialDelay,
            period,
            TimeUnit.SECONDS
        );

        config.log("[Monitoring] Hourly aggregation job scheduled (runs at 5 minutes past each hour)");
    }

    /**
     * Aggregate all historical data (run on startup)
     */
    public void aggregateAllHistory() {
        String sql = "MERGE INTO idoc_hourly_stats " +
                    "(stat_hour, idoc_type, total_received, total_processed, total_kafka_published, " +
                    "total_errors, avg_processing_time_ms, total_xml_bytes, total_json_bytes) " +
                    "KEY(stat_hour, idoc_type) " +
                    "SELECT " +
                    "DATEADD('HOUR', DATEDIFF('HOUR', TIMESTAMP '1970-01-01 00:00:00', timestamp), TIMESTAMP '1970-01-01 00:00:00') as stat_hour, " +
                    "idoc_type, " +
                    "COUNT(*) as total_received, " +
                    "COUNT(CASE WHEN stage = 'PROCESSED' AND status = 'SUCCESS' THEN 1 END) as total_processed, " +
                    "COUNT(CASE WHEN stage = 'KAFKA_PUBLISH' AND status = 'SUCCESS' THEN 1 END) as total_kafka_published, " +
                    "COUNT(CASE WHEN event_type = 'ERROR' THEN 1 END) as total_errors, " +
                    "AVG(processing_time_ms) as avg_processing_time_ms, " +
                    "SUM(xml_size_bytes) as total_xml_bytes, " +
                    "SUM(json_size_bytes) as total_json_bytes " +
                    "FROM idoc_events " +
                    "GROUP BY DATEADD('HOUR', DATEDIFF('HOUR', TIMESTAMP '1970-01-01 00:00:00', timestamp), TIMESTAMP '1970-01-01 00:00:00'), idoc_type";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            int rows = stmt.executeUpdate(sql);
            config.log("[Monitoring] Aggregated all historical data: " + rows + " hourly stat rows");

        } catch (Exception e) {
            config.logError("[Monitoring] Failed to aggregate historical data", e);
        }
    }

    /**
     * Aggregate last hour's data (run periodically)
     */
    public void aggregate() {
        // Explicitly specify columns to exclude auto-increment id column
        String sql = "MERGE INTO idoc_hourly_stats " +
                    "(stat_hour, idoc_type, total_received, total_processed, total_kafka_published, " +
                    "total_errors, avg_processing_time_ms, total_xml_bytes, total_json_bytes) " +
                    "KEY(stat_hour, idoc_type) " +
                    "SELECT " +
                    "DATEADD('HOUR', DATEDIFF('HOUR', TIMESTAMP '1970-01-01 00:00:00', timestamp), TIMESTAMP '1970-01-01 00:00:00') as stat_hour, " +
                    "idoc_type, " +
                    "COUNT(*) as total_received, " +
                    "COUNT(CASE WHEN stage = 'PROCESSED' AND status = 'SUCCESS' THEN 1 END) as total_processed, " +
                    "COUNT(CASE WHEN stage = 'KAFKA_PUBLISH' AND status = 'SUCCESS' THEN 1 END) as total_kafka_published, " +
                    "COUNT(CASE WHEN event_type = 'ERROR' THEN 1 END) as total_errors, " +
                    "AVG(processing_time_ms) as avg_processing_time_ms, " +
                    "SUM(xml_size_bytes) as total_xml_bytes, " +
                    "SUM(json_size_bytes) as total_json_bytes " +
                    "FROM idoc_events " +
                    "WHERE timestamp >= DATEADD('HOUR', -1, CURRENT_TIMESTAMP) " +
                    "AND timestamp < DATEADD('HOUR', DATEDIFF('HOUR', TIMESTAMP '1970-01-01 00:00:00', CURRENT_TIMESTAMP), TIMESTAMP '1970-01-01 00:00:00') " +
                    "GROUP BY DATEADD('HOUR', DATEDIFF('HOUR', TIMESTAMP '1970-01-01 00:00:00', timestamp), TIMESTAMP '1970-01-01 00:00:00'), idoc_type";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            int rows = stmt.executeUpdate(sql);
            config.log("[Monitoring] Aggregated " + rows + " hourly stats");

        } catch (Exception e) {
            config.logError("[Monitoring] Failed to aggregate hourly stats", e);
        }
    }

    public void stop() {
        shutdown();
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    private long getDelayUntilNextHour() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        return ChronoUnit.SECONDS.between(now, nextHour);
    }
}

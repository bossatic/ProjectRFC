package org.dataingest.rfc.server.monitoring.db;

import org.dataingest.rfc.server.config.IDocCaptureConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background job to clean up old events from H2 database
 */
public class H2CleanupJob {
    private final DataSource dataSource;
    private final IDocCaptureConfig config;
    private final int retentionDays;
    private final ScheduledExecutorService scheduler;

    public H2CleanupJob(DataSource dataSource, IDocCaptureConfig config, int retentionDays) {
        this.dataSource = dataSource;
        this.config = config;
        this.retentionDays = retentionDays;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        long initialDelay = getDelayUntil2AM();
        long period = 24 * 60 * 60; // 24 hours in seconds

        scheduler.scheduleAtFixedRate(
            this::cleanup,
            initialDelay,
            period,
            TimeUnit.SECONDS
        );

        config.log("[Monitoring] Cleanup job scheduled (runs daily at 2 AM)");
    }

    public void cleanup() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Delete events older than retention period
            String deleteSql = String.format(
                "DELETE FROM idoc_events WHERE timestamp < DATEADD('DAY', -%d, CURRENT_TIMESTAMP)",
                retentionDays
            );
            int deleted = stmt.executeUpdate(deleteSql);
            config.log("[Monitoring] Cleaned up " + deleted + " old events from H2");

            // Delete old hourly stats
            deleteSql = String.format(
                "DELETE FROM idoc_hourly_stats WHERE stat_hour < DATEADD('DAY', -%d, CURRENT_TIMESTAMP)",
                retentionDays
            );
            deleted = stmt.executeUpdate(deleteSql);
            config.log("[Monitoring] Cleaned up " + deleted + " old hourly stats from H2");

        } catch (Exception e) {
            config.logError("[Monitoring] Failed to cleanup H2 database", e);
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

    private long getDelayUntil2AM() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next2AM = now.withHour(2).withMinute(0).withSecond(0);
        if (now.isAfter(next2AM)) {
            next2AM = next2AM.plusDays(1);
        }
        return ChronoUnit.SECONDS.between(now, next2AM);
    }
}

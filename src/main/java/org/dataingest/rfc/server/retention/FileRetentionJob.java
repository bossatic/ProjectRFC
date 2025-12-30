package org.dataingest.rfc.server.retention;

import org.dataingest.rfc.server.config.IDocCaptureConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Background job to clean up old XML and JSON files from disk
 */
public class FileRetentionJob {
    private final IDocCaptureConfig config;
    private final int retentionDays;
    private final int checkIntervalHours;
    private final ScheduledExecutorService scheduler;

    public FileRetentionJob(IDocCaptureConfig config) {
        this.config = config;
        this.retentionDays = config.getFileRetentionDays();
        this.checkIntervalHours = config.getFileRetentionCheckIntervalHours();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Start the scheduled cleanup job
     */
    public void start() {
        long initialDelayMinutes = 1; // Start after 1 minute
        long periodHours = checkIntervalHours;

        scheduler.scheduleAtFixedRate(
            this::cleanup,
            initialDelayMinutes,
            periodHours,
            TimeUnit.HOURS
        );

        config.log(String.format("[FileRetention] Cleanup job started (runs every %d hours, retention: %d days)",
            checkIntervalHours, retentionDays));
    }

    /**
     * Perform cleanup of old files
     */
    public void cleanup() {
        config.log("[FileRetention] Starting file cleanup...");

        AtomicInteger totalDeleted = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);

        // Calculate cutoff time
        Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        // Clean XML directory
        String xmlDir = config.getOutputDirectory();
        int xmlDeleted = cleanupDirectory(xmlDir, cutoffTime, "*.xml");
        totalDeleted.addAndGet(xmlDeleted);

        // Clean JSON directory (if different from XML directory)
        String jsonDir = config.getJsonOutputDirectory();
        if (!jsonDir.equals(xmlDir)) {
            int jsonDeleted = cleanupDirectory(jsonDir, cutoffTime, "*.json");
            totalDeleted.addAndGet(jsonDeleted);
        } else {
            // If same directory, also clean JSON files
            int jsonDeleted = cleanupDirectory(jsonDir, cutoffTime, "*.json");
            totalDeleted.addAndGet(jsonDeleted);
        }

        config.log(String.format("[FileRetention] Cleanup completed: %d files deleted", totalDeleted.get()));
    }

    /**
     * Clean up files in a specific directory
     */
    private int cleanupDirectory(String directoryPath, Instant cutoffTime, String filePattern) {
        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            config.log(String.format("[FileRetention] Directory not found or not a directory: %s", directoryPath));
            return 0;
        }

        AtomicInteger deletedCount = new AtomicInteger(0);

        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath), 1)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> matchesPattern(path, filePattern))
                .forEach(path -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                        Instant fileTime = attrs.lastModifiedTime().toInstant();

                        if (fileTime.isBefore(cutoffTime)) {
                            Files.delete(path);
                            deletedCount.incrementAndGet();
                            config.log(String.format("[FileRetention] Deleted old file: %s (age: %d days)",
                                path.getFileName(),
                                ChronoUnit.DAYS.between(fileTime, Instant.now())));
                        }
                    } catch (IOException e) {
                        config.logError("[FileRetention] Failed to delete file: " + path, e);
                    }
                });
        } catch (IOException e) {
            config.logError("[FileRetention] Failed to scan directory: " + directoryPath, e);
        }

        return deletedCount.get();
    }

    /**
     * Check if file matches the pattern (e.g., *.xml, *.json)
     */
    private boolean matchesPattern(Path path, String pattern) {
        String fileName = path.getFileName().toString().toLowerCase();
        String extension = pattern.substring(pattern.lastIndexOf('.'));
        return fileName.endsWith(extension);
    }

    /**
     * Stop the cleanup job
     */
    public void stop() {
        shutdown();
    }

    /**
     * Shutdown the scheduler
     */
    public void shutdown() {
        config.log("[FileRetention] Shutting down cleanup job...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

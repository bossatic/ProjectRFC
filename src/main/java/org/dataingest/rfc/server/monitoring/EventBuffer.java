package org.dataingest.rfc.server.monitoring;

import org.dataingest.rfc.server.config.IDocCaptureConfig;
import org.dataingest.rfc.server.monitoring.db.H2EventWriter;
import org.dataingest.rfc.server.monitoring.events.MonitoringEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Async event buffer for batching writes to H2 database
 */
public class EventBuffer {
    private final BlockingQueue<MonitoringEvent> buffer;
    private final H2EventWriter writer;
    private final ScheduledExecutorService scheduler;
    private final IDocCaptureConfig config;
    private final int batchSize;
    private final int flushIntervalSeconds;
    private final AtomicBoolean acceptingEvents = new AtomicBoolean(true);

    public EventBuffer(H2EventWriter writer, IDocCaptureConfig config, int batchSize, int flushIntervalSeconds) {
        this.buffer = new LinkedBlockingQueue<>(10000);
        this.writer = writer;
        this.config = config;
        this.batchSize = batchSize;
        this.flushIntervalSeconds = flushIntervalSeconds;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
            this::flush,
            flushIntervalSeconds,
            flushIntervalSeconds,
            TimeUnit.SECONDS
        );
        config.log("[Monitoring] Event buffer started (batch size: " + batchSize +
                   ", flush interval: " + flushIntervalSeconds + "s)");
    }

    public void add(MonitoringEvent event) {
        if (!acceptingEvents.get()) {
            return;
        }

        if (!buffer.offer(event)) {
            config.logError("[Monitoring] Event buffer full, dropping event", null);
            return;
        }

        // Flush if buffer is large
        if (buffer.size() >= batchSize) {
            scheduler.execute(this::flush);
        }
    }

    public void flush() {
        List<MonitoringEvent> batch = new ArrayList<>(batchSize);
        buffer.drainTo(batch, batchSize);

        if (!batch.isEmpty()) {
            writer.batchInsert(batch);
        }
    }

    public int size() {
        return buffer.size();
    }

    public void stopAccepting() {
        acceptingEvents.set(false);
    }

    public void shutdown() {
        stopAccepting();
        config.log("[Monitoring] Shutting down event buffer, flushing remaining events...");

        // Flush all remaining events
        flush();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        config.log("[Monitoring] Event buffer shut down");
    }
}

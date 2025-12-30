package org.dataingest.rfc.server.monitoring;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.dataingest.rfc.server.config.IDocCaptureConfig;
import org.dataingest.rfc.server.monitoring.db.*;
import org.dataingest.rfc.server.monitoring.web.EmbeddedWebServer;

import javax.sql.DataSource;

/**
 * Central manager for the monitoring system
 * Handles initialization, startup, and shutdown of all monitoring components
 */
public class MonitoringManager {
    private final IDocCaptureConfig config;

    // Core components
    private MetricsStore metricsStore;
    private KafkaProducer<String, String> monitoringKafkaProducer;
    private MonitoringEventPublisher eventPublisher;
    private EventBuffer eventBuffer;

    // Database components
    private DataSource dataSource;
    private H2EventWriter eventWriter;
    private H2QueryService queryService;
    private H2CleanupJob cleanupJob;
    private HourlyAggregationJob aggregationJob;

    // Web components
    private EmbeddedWebServer webServer;
    private H2ConsoleServer h2ConsoleServer;

    private boolean initialized = false;
    private boolean started = false;

    public MonitoringManager(IDocCaptureConfig config) {
        this.config = config;
    }

    /**
     * Initialize all monitoring components
     */
    public void initialize() throws Exception {
        if (!config.isMonitoringEnabled()) {
            config.log("Monitoring is disabled in configuration");
            return;
        }

        config.log("Initializing monitoring system...");

        try {
            // 1. Create metrics store (in-memory)
            metricsStore = new MetricsStore();
            config.log("MetricsStore initialized");

            // 2. Initialize H2 database if enabled
            if (config.isH2Enabled()) {
                dataSource = H2DataSourceFactory.create(config);
                eventWriter = new H2EventWriter(dataSource, config);
                queryService = new H2QueryService(dataSource);
                config.log("H2 Database initialized at: " + config.getH2DatabasePath());

                // Load historical metrics from database into MetricsStore
                metricsStore.initializeFromDatabase(dataSource);
                config.log("MetricsStore loaded historical data from database");
            }

            // 3. Create event buffer
            if (eventWriter != null) {
                eventBuffer = new EventBuffer(
                    eventWriter,
                    config,
                    config.getH2BatchSize(),
                    config.getH2BatchIntervalSeconds()
                );
                config.log("EventBuffer initialized");
            }

            // 4. Create Kafka producer for monitoring events if enabled
            if (config.isMonitoringKafkaEnabled()) {
                java.util.Properties kafkaProps = new java.util.Properties();
                kafkaProps.put("bootstrap.servers", config.getKafkaBootstrapServers());
                kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
                kafkaProps.put("acks", "1");
                kafkaProps.put("retries", 3);
                monitoringKafkaProducer = new KafkaProducer<>(kafkaProps);
            }

            // 5. Create monitoring event publisher
            eventPublisher = new MonitoringEventPublisher(eventBuffer, monitoringKafkaProducer, config);
            config.log("MonitoringEventPublisher initialized");

            // 6. Initialize background jobs
            if (config.isH2Enabled() && dataSource != null) {
                cleanupJob = new H2CleanupJob(dataSource, config, config.getH2RetentionDays());

                if (config.isH2AggregationEnabled()) {
                    aggregationJob = new HourlyAggregationJob(dataSource, config);
                }
                config.log("Background jobs initialized");
            }

            // 7. Initialize web server if dashboard is enabled
            if (config.isMonitoringDashboardEnabled() && metricsStore != null) {
                webServer = new EmbeddedWebServer(config, metricsStore, queryService);
                config.log("Web server initialized");
            }

            // 8. Initialize H2 Console if enabled
            if (config.isH2ConsoleEnabled()) {
                h2ConsoleServer = new H2ConsoleServer(config);
                config.log("H2 Console server initialized");
            }

            initialized = true;
            config.log("Monitoring system initialized successfully");

        } catch (Exception e) {
            config.logError("Failed to initialize monitoring system", e);
            throw e;
        }
    }

    /**
     * Start all monitoring components
     */
    public void start() throws Exception {
        if (!config.isMonitoringEnabled()) {
            return;
        }

        if (!initialized) {
            throw new IllegalStateException("MonitoringManager must be initialized before starting");
        }

        config.log("Starting monitoring system...");

        try {
            // 1. Start event buffer
            if (eventBuffer != null) {
                eventBuffer.start();
                config.log("EventBuffer started");
            }

            // 2. Start background jobs
            if (cleanupJob != null) {
                cleanupJob.start();
                config.log("H2 cleanup job started");
            }

            if (aggregationJob != null) {
                aggregationJob.start();
                config.log("Hourly aggregation job started");
            }

            // 3. Start web server
            if (webServer != null) {
                webServer.start();
                config.log("Web dashboard started at http://" +
                    config.getMonitoringDashboardHost() + ":" +
                    config.getMonitoringDashboardPort());
            }

            // 4. Start H2 Console
            if (h2ConsoleServer != null) {
                h2ConsoleServer.start();
            }

            started = true;
            config.log("Monitoring system started successfully");

        } catch (Exception e) {
            config.logError("Failed to start monitoring system", e);
            throw e;
        }
    }

    /**
     * Shutdown all monitoring components gracefully
     */
    public void shutdown() {
        if (!config.isMonitoringEnabled() || !started) {
            return;
        }

        config.log("Shutting down monitoring system...");

        try {
            // 1. Stop web server
            if (webServer != null) {
                webServer.stop();
                config.log("Web server stopped");
            }

            // 2. Stop H2 Console
            if (h2ConsoleServer != null) {
                h2ConsoleServer.stop();
            }

            // 3. Stop background jobs
            if (aggregationJob != null) {
                aggregationJob.stop();
                config.log("Aggregation job stopped");
            }

            if (cleanupJob != null) {
                cleanupJob.stop();
                config.log("Cleanup job stopped");
            }

            // 4. Flush and stop event buffer
            if (eventBuffer != null) {
                eventBuffer.shutdown();
                config.log("Event buffer flushed and stopped");
            }

            // 5. Close Kafka producer
            if (monitoringKafkaProducer != null) {
                monitoringKafkaProducer.close();
                config.log("Monitoring Kafka producer closed");
            }

            // 6. Close database connections
            if (dataSource != null) {
                if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                    ((com.zaxxer.hikari.HikariDataSource) dataSource).close();
                    config.log("Database connections closed");
                }
            }

            config.log("Monitoring system shutdown complete");

        } catch (Exception e) {
            config.logError("Error during monitoring system shutdown", e);
        }
    }

    // Getters for components (used by integration code)

    public MetricsStore getMetricsStore() {
        return metricsStore;
    }

    public MonitoringEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    public H2QueryService getQueryService() {
        return queryService;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isStarted() {
        return started;
    }

    public EmbeddedWebServer getWebServer() {
        return webServer;
    }
}

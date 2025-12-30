package org.dataingest.rfc.server.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Manages IDoc capture configuration from external properties file
 */
public class IDocCaptureConfig {
    private static final String DEFAULT_CONFIG_FILE = "idoc_capture.properties";

    private Properties properties;
    private String outputDirectory;
    private String idocDocumentationDirectory;
    private String jsonOutputDirectory;
    private String jcoServerName;
    private String jcoRepositoryDestination;
    private boolean loggingEnabled;
    private boolean useTimestamp;
    private boolean jsonConversionEnabled;
    private boolean summaryFileEnabled;
    private String summaryFileName;
    private int documentationReloadInterval;
    private boolean kafkaEnabled;
    private String kafkaBootstrapServers;
    private String kafkaTopicPrefix;
    private boolean kafkaPushJson;
    private String kafkaAcks;
    private int kafkaRetries;
    private int kafkaBatchSize;
    private int kafkaLingerMs;

    // Monitoring configuration
    private boolean monitoringEnabled;
    private boolean monitoringDashboardEnabled;
    private int monitoringDashboardPort;
    private String monitoringDashboardHost;
    private boolean monitoringKafkaEnabled;
    private String monitoringKafkaTopic;
    private String monitoringEventDetail;

    // H2 Database configuration
    private boolean h2Enabled;
    private String h2DatabasePath;
    private int h2PoolSize;
    private int h2RetentionDays;
    private int h2BatchSize;
    private int h2BatchIntervalSeconds;
    private boolean h2ConsoleEnabled;
    private int h2ConsolePort;
    private boolean h2AggregationEnabled;

    // File retention configuration
    private boolean fileRetentionEnabled;
    private int fileRetentionDays;
    private int fileRetentionCheckIntervalHours;

    /**
     * Load configuration from default properties file
     */
    public IDocCaptureConfig() throws IOException {
        this(DEFAULT_CONFIG_FILE);
    }

    /**
     * Load configuration from specified properties file
     */
    public IDocCaptureConfig(String configFilePath) throws IOException {
        properties = new Properties();

        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            properties.load(fis);
            parseProperties();
            log("Configuration loaded from: " + configFilePath);
        } catch (IOException e) {
            throw new IOException("Failed to load configuration file: " + configFilePath, e);
        }
    }

    private void parseProperties() {
        outputDirectory = properties.getProperty("output.directory", "D:/RFC_SERVER/IDocs/captured");
        idocDocumentationDirectory = properties.getProperty("idoc.documentation.directory", "D:/RFC_SERVER/IDocs/documentation");
        jsonOutputDirectory = properties.getProperty("json.output.directory", "");

        // If JSON output directory is empty, use output directory
        if (jsonOutputDirectory.trim().isEmpty()) {
            jsonOutputDirectory = outputDirectory;
        }

        jcoServerName = properties.getProperty("jco.server.name", "IDOC_SERVER");
        jcoRepositoryDestination = properties.getProperty("jco.repository.destination", "SAP_SYSTEM");
        loggingEnabled = Boolean.parseBoolean(properties.getProperty("logging.enabled", "true"));
        useTimestamp = Boolean.parseBoolean(properties.getProperty("file.use.timestamp", "true"));
        jsonConversionEnabled = Boolean.parseBoolean(properties.getProperty("json.conversion.enabled", "true"));
        summaryFileEnabled = Boolean.parseBoolean(properties.getProperty("summary.file.enabled", "true"));
        summaryFileName = properties.getProperty("summary.file.name", "idoc_capture_summary.txt");
        documentationReloadInterval = Integer.parseInt(properties.getProperty("documentation.reload.interval", "60"));

        // Kafka configuration
        kafkaEnabled = Boolean.parseBoolean(properties.getProperty("kafka.enabled", "false"));
        kafkaBootstrapServers = properties.getProperty("kafka.bootstrap.servers", "localhost:9092");
        kafkaTopicPrefix = properties.getProperty("kafka.topic.prefix", "idoc_");
        kafkaPushJson = Boolean.parseBoolean(properties.getProperty("kafka.push.json", "true"));
        kafkaAcks = properties.getProperty("kafka.acks", "all");
        kafkaRetries = Integer.parseInt(properties.getProperty("kafka.retries", "3"));
        kafkaBatchSize = Integer.parseInt(properties.getProperty("kafka.batch.size", "16384"));
        kafkaLingerMs = Integer.parseInt(properties.getProperty("kafka.linger.ms", "10"));

        // Monitoring configuration
        monitoringEnabled = Boolean.parseBoolean(properties.getProperty("monitoring.enabled", "false"));
        monitoringDashboardEnabled = Boolean.parseBoolean(properties.getProperty("monitoring.dashboard.enabled", "true"));
        monitoringDashboardPort = Integer.parseInt(properties.getProperty("monitoring.dashboard.port", "8080"));
        monitoringDashboardHost = properties.getProperty("monitoring.dashboard.host", "localhost");
        monitoringKafkaEnabled = Boolean.parseBoolean(properties.getProperty("monitoring.kafka.enabled", "true"));
        monitoringKafkaTopic = properties.getProperty("monitoring.kafka.topic", "idoc_monitoring_events");
        monitoringEventDetail = properties.getProperty("monitoring.event.detail", "DETAILED");

        // H2 Database configuration
        h2Enabled = Boolean.parseBoolean(properties.getProperty("h2.enabled", "true"));
        h2DatabasePath = properties.getProperty("h2.database.path", "./data/idoc_monitoring");
        h2PoolSize = Integer.parseInt(properties.getProperty("h2.pool.size", "10"));
        h2RetentionDays = Integer.parseInt(properties.getProperty("h2.retention.days", "7"));
        h2BatchSize = Integer.parseInt(properties.getProperty("h2.batch.size", "100"));
        h2BatchIntervalSeconds = Integer.parseInt(properties.getProperty("h2.batch.interval.seconds", "5"));
        h2ConsoleEnabled = Boolean.parseBoolean(properties.getProperty("h2.console.enabled", "false"));
        h2ConsolePort = Integer.parseInt(properties.getProperty("h2.console.port", "8082"));
        h2AggregationEnabled = Boolean.parseBoolean(properties.getProperty("h2.aggregation.enabled", "true"));

        // File retention configuration
        fileRetentionEnabled = Boolean.parseBoolean(properties.getProperty("file.retention.enabled", "false"));
        fileRetentionDays = Integer.parseInt(properties.getProperty("file.retention.days", "7"));
        fileRetentionCheckIntervalHours = Integer.parseInt(properties.getProperty("file.retention.check.interval.hours", "24"));

        // Normalize path separators
        outputDirectory = normalizePath(outputDirectory);
        idocDocumentationDirectory = normalizePath(idocDocumentationDirectory);
        jsonOutputDirectory = normalizePath(jsonOutputDirectory);
    }

    private String normalizePath(String path) {
        return path.replace("/", java.io.File.separator).replace("\\", java.io.File.separator);
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public String getIdocDocumentationDirectory() {
        return idocDocumentationDirectory;
    }

    public String getJsonOutputDirectory() {
        return jsonOutputDirectory;
    }

    public String getJcoServerName() {
        return jcoServerName;
    }

    public String getJcoRepositoryDestination() {
        return jcoRepositoryDestination;
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public boolean isUseTimestamp() {
        return useTimestamp;
    }

    public boolean isJsonConversionEnabled() {
        return jsonConversionEnabled;
    }

    public boolean isSummaryFileEnabled() {
        return summaryFileEnabled;
    }

    public String getSummaryFileName() {
        return summaryFileName;
    }

    public int getDocumentationReloadInterval() {
        return documentationReloadInterval;
    }

    public boolean isKafkaEnabled() {
        return kafkaEnabled;
    }

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public String getKafkaTopicPrefix() {
        return kafkaTopicPrefix;
    }

    public boolean isKafkaPushJson() {
        return kafkaPushJson;
    }

    public String getKafkaAcks() {
        return kafkaAcks;
    }

    public int getKafkaRetries() {
        return kafkaRetries;
    }

    public int getKafkaBatchSize() {
        return kafkaBatchSize;
    }

    public int getKafkaLingerMs() {
        return kafkaLingerMs;
    }

    // Monitoring getters
    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }

    public boolean isMonitoringDashboardEnabled() {
        return monitoringDashboardEnabled;
    }

    public int getMonitoringDashboardPort() {
        return monitoringDashboardPort;
    }

    public String getMonitoringDashboardHost() {
        return monitoringDashboardHost;
    }

    public boolean isMonitoringKafkaEnabled() {
        return monitoringKafkaEnabled;
    }

    public String getMonitoringKafkaTopic() {
        return monitoringKafkaTopic;
    }

    public String getMonitoringEventDetail() {
        return monitoringEventDetail;
    }

    // H2 Database getters
    public boolean isH2Enabled() {
        return h2Enabled;
    }

    public String getH2DatabasePath() {
        return h2DatabasePath;
    }

    public int getH2PoolSize() {
        return h2PoolSize;
    }

    public int getH2RetentionDays() {
        return h2RetentionDays;
    }

    public int getH2BatchSize() {
        return h2BatchSize;
    }

    public int getH2BatchIntervalSeconds() {
        return h2BatchIntervalSeconds;
    }

    public boolean isH2ConsoleEnabled() {
        return h2ConsoleEnabled;
    }

    public int getH2ConsolePort() {
        return h2ConsolePort;
    }

    public boolean isH2AggregationEnabled() {
        return h2AggregationEnabled;
    }

    // File retention getters
    public boolean isFileRetentionEnabled() {
        return fileRetentionEnabled;
    }

    public int getFileRetentionDays() {
        return fileRetentionDays;
    }

    public int getFileRetentionCheckIntervalHours() {
        return fileRetentionCheckIntervalHours;
    }

    public void log(String message) {
        if (loggingEnabled) {
            System.out.println("[IDocCapture] " + message);
        }
    }

    public void logError(String message, Exception e) {
        System.err.println("[IDocCapture ERROR] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}

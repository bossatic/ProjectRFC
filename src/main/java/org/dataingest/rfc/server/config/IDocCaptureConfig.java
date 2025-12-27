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

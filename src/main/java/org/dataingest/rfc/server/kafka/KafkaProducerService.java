package org.dataingest.rfc.server.kafka;

import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.dataingest.rfc.server.config.IDocCaptureConfig;
import org.dataingest.rfc.server.monitoring.MetricsStore;
import org.dataingest.rfc.server.monitoring.MonitoringEventPublisher;
import org.dataingest.rfc.server.monitoring.events.*;

import java.time.Instant;

/**
 * Kafka Producer Service for publishing IDoc JSON to Kafka topics
 */
public class KafkaProducerService {

    private KafkaProducer<String, String> producer;
    private IDocCaptureConfig config;
    private boolean initialized = false;
    private MetricsStore metricsStore;
    private MonitoringEventPublisher eventPublisher;

    public KafkaProducerService(IDocCaptureConfig config) {
        this.config = config;

        if (config.isKafkaEnabled()) {
            initialize();
        }
    }

    /**
     * Initialize Kafka producer
     */
    private void initialize() {
        try {
            Properties props = new Properties();
            props.put("bootstrap.servers", config.getKafkaBootstrapServers());
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("acks", config.getKafkaAcks());
            props.put("retries", config.getKafkaRetries());
            props.put("batch.size", config.getKafkaBatchSize());
            props.put("linger.ms", config.getKafkaLingerMs());

            this.producer = new KafkaProducer<>(props);
            this.initialized = true;

            config.log("Kafka producer initialized: " + config.getKafkaBootstrapServers());
        } catch (Exception e) {
            config.logError("Failed to initialize Kafka producer", e);
            this.initialized = false;
        }
    }

    /**
     * Set monitoring components
     */
    public void setMonitoring(MetricsStore metricsStore, MonitoringEventPublisher eventPublisher) {
        this.metricsStore = metricsStore;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publish JSON message to Kafka
     *
     * @param idocType      IDoc type (e.g., ORDERS05)
     * @param docNum        Document number (for message key)
     * @param jsonContent   JSON content to publish
     */
    public void publishJson(String idocType, String docNum, String jsonContent) {
        if (!initialized) {
            config.log("Kafka is not enabled or not initialized");
            return;
        }

        new Thread(() -> {
            Instant startTime = Instant.now();
            try {
                String topic = config.getKafkaTopicPrefix() + idocType.toLowerCase();
                String key = docNum != null ? docNum : "";

                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, jsonContent);

                Future<RecordMetadata> future = producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        config.logError("Failed to publish to Kafka topic: " + topic, exception);

                        // Update monitoring - Kafka error
                        if (metricsStore != null && eventPublisher != null) {
                            metricsStore.incrementError(ErrorStage.KAFKA_PUBLISH);
                            ErrorEvent errorEvent = new ErrorEvent(ErrorStage.KAFKA_PUBLISH, idocType, docNum,
                                exception.getMessage(), exception.toString(), true);
                            eventPublisher.publishAsync(errorEvent);
                        }
                    } else {
                        config.log("Published to Kafka - Topic: " + metadata.topic() +
                                  ", Partition: " + metadata.partition() +
                                  ", Offset: " + metadata.offset());

                        // Update monitoring - Kafka published successfully
                        if (metricsStore != null && eventPublisher != null) {
                            metricsStore.incrementKafkaPublished();

                            long latencyMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
                            KafkaPublishedEvent event = new KafkaPublishedEvent(idocType, docNum,
                                metadata.topic(), metadata.partition(), metadata.offset(), latencyMs);
                            eventPublisher.publishAsync(event);
                        }
                    }
                });

                // Don't wait for the future, publish asynchronously
            } catch (Exception e) {
                config.logError("Error publishing to Kafka", e);

                // Update monitoring - Kafka error
                if (metricsStore != null && eventPublisher != null) {
                    metricsStore.incrementError(ErrorStage.KAFKA_PUBLISH);
                    ErrorEvent errorEvent = new ErrorEvent(ErrorStage.KAFKA_PUBLISH, idocType, docNum,
                        e.getMessage(), e.toString(), false);
                    eventPublisher.publishAsync(errorEvent);
                }
            }
        }).start();
    }

    /**
     * Close the Kafka producer
     */
    public void close() {
        if (producer != null) {
            try {
                producer.flush();
                producer.close();
                config.log("Kafka producer closed");
            } catch (Exception e) {
                config.logError("Error closing Kafka producer", e);
            }
        }
    }

    /**
     * Check if Kafka is initialized and ready
     */
    public boolean isInitialized() {
        return initialized;
    }
}

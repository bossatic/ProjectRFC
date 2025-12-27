package org.dataingest.rfc.server.kafka;

import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.dataingest.rfc.server.config.IDocCaptureConfig;

/**
 * Kafka Producer Service for publishing IDoc JSON to Kafka topics
 */
public class KafkaProducerService {

    private KafkaProducer<String, String> producer;
    private IDocCaptureConfig config;
    private boolean initialized = false;

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
            try {
                String topic = config.getKafkaTopicPrefix() + idocType.toLowerCase();
                String key = docNum != null ? docNum : "";

                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, jsonContent);

                Future<RecordMetadata> future = producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        config.logError("Failed to publish to Kafka topic: " + topic, exception);
                    } else {
                        config.log("Published to Kafka - Topic: " + metadata.topic() +
                                  ", Partition: " + metadata.partition() +
                                  ", Offset: " + metadata.offset());
                    }
                });

                // Don't wait for the future, publish asynchronously
            } catch (Exception e) {
                config.logError("Error publishing to Kafka", e);
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

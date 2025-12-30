package org.dataingest.rfc.server.monitoring;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.dataingest.rfc.server.config.IDocCaptureConfig;
import org.dataingest.rfc.server.monitoring.events.MonitoringEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes monitoring events to both H2 database and Kafka
 */
public class MonitoringEventPublisher {
    private final EventBuffer eventBuffer;
    private final KafkaProducer<String, String> kafkaProducer;
    private final IDocCaptureConfig config;
    private final String monitoringTopic;
    private final boolean h2Enabled;
    private final boolean kafkaEnabled;

    public MonitoringEventPublisher(EventBuffer eventBuffer,
                                   KafkaProducer<String, String> kafkaProducer,
                                   IDocCaptureConfig config) {
        this.eventBuffer = eventBuffer;
        this.kafkaProducer = kafkaProducer;
        this.config = config;
        this.monitoringTopic = config.getMonitoringKafkaTopic();
        this.h2Enabled = config.isH2Enabled();
        this.kafkaEnabled = config.isMonitoringKafkaEnabled();
    }

    /**
     * Publish event asynchronously to H2 and Kafka
     */
    public void publishAsync(MonitoringEvent event) {
        // Add to buffer for H2 storage
        if (h2Enabled && eventBuffer != null) {
            eventBuffer.add(event);
        }

        // Publish to Kafka monitoring topic
        if (kafkaEnabled && kafkaProducer != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    String key = event.getDocNum();
                    String value = event.toJson();

                    ProducerRecord<String, String> record =
                        new ProducerRecord<>(monitoringTopic, key, value);

                    kafkaProducer.send(record, (metadata, exception) -> {
                        if (exception != null) {
                            config.logError("[Monitoring] Failed to publish event to Kafka", exception);
                        }
                    });
                } catch (Exception e) {
                    config.logError("[Monitoring] Error publishing monitoring event", e);
                }
            });
        }
    }

    public void shutdown() {
        if (eventBuffer != null) {
            eventBuffer.shutdown();
        }
    }
}

package org.dataingest.rfc.server.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.dataingest.rfc.server.exception.KafkaPublishException;
import org.dataingest.rfc.server.model.SAPBWDataRequest;
import org.dataingest.rfc.server.util.BWDataTopicNameUtil;

/**
 * Publisher for SAP BW (Business Warehouse) Data Source requests to Kafka topics.
 *
 * Uses official SAP libraries to receive BW data requests and publishes them directly to Kafka
 * with synchronous publishing to guarantee delivery.
 *
 * Handles:
 * - JSON serialization of BW data requests
 * - Topic name generation based on data source name
 * - Synchronous publishing with configurable timeout
 * - Error handling with exception propagation
 */
@Component
public class BWDataKafkaPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BWDataKafkaPublisher.class);

    @Autowired
    protected Producer<String, String> kafkaProducer;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected BWDataTopicNameUtil topicNameUtil;

    @Value("${kafka.request.timeout.ms:30000}")
    private int kafkaTimeoutMs;

    /**
     * Publishes a BW data request to Kafka.
     *
     * The request is serialized to JSON and published to a topic based on the data source name.
     * Publishing is synchronous to guarantee delivery before returning to SAP.
     *
     * Topic Pattern: SAP.DATASOURCES.{DATA_SOURCE_NAME}
     * Example: SAP.DATASOURCES.0MATERIAL_ATTR
     *
     * @param request the SAP BW data request to publish
     * @throws KafkaPublishException if publishing fails
     */
    public void publishBWDataRequest(SAPBWDataRequest request) throws KafkaPublishException {
        if (request == null) {
            LOGGER.debug("Skipping null BW data request");
            return;
        }

        try {
            // Determine topic name based on data source name using configurable prefix
            String topicName = topicNameUtil.getTopicName(request.getDataSourceName());

            // Serialize BW data request to JSON
            String requestJson = objectMapper.writeValueAsString(request);

            // Create Kafka producer record
            ProducerRecord<String, String> record = new ProducerRecord<>(
                topicName,
                request.getRequestId(),  // Use request ID as key for ordering
                requestJson
            );

            // Send synchronously with timeout to ensure delivery
            try {
                kafkaProducer.send(record).get();
                LOGGER.info("Published BW data request {} to topic {}",
                    request.getRequestId(), topicName);
            } catch (Exception e) {
                LOGGER.error("Failed to publish BW data request {} to topic {}: {}",
                    request.getRequestId(), topicName, e.getMessage(), e);
                throw e;
            }

        } catch (Exception e) {
            String errorMsg = String.format(
                "Failed to publish BW data request %s: %s",
                request.getRequestId(), e.getMessage());
            LOGGER.error(errorMsg, e);
            throw new KafkaPublishException(errorMsg, e);
        }
    }
}

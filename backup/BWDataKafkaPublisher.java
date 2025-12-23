package org.dataingest.rfc.server.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.talend.sap.bw.ISAPBWDataRequest;
import org.dataingest.rfc.server.exception.KafkaPublishException;
import org.dataingest.rfc.server.util.BWDataTopicNameUtil;

/**
 * Publisher for SAP BW Data Source requests to Kafka topics.
 *
 * Handles synchronous publishing of BW data requests directly to Kafka with proper error handling.
 */
@Component
public class BWDataKafkaPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BWDataKafkaPublisher.class);

    @Autowired
    protected Producer<String, String> kafkaProducer;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${kafka.request.timeout.ms:30000}")
    private int kafkaTimeoutMs;

    /**
     * Publishes a BW data request to Kafka.
     *
     * The request is serialized to JSON and published to a topic based on the data source name.
     * Publishing is synchronous to ensure delivery before returning.
     *
     * @param dataRequest the BW data request to publish
     * @throws KafkaPublishException if publishing fails
     */
    public void publish(ISAPBWDataRequest dataRequest) throws KafkaPublishException {
        if (dataRequest == null) {
            LOGGER.debug("Skipping null BW data request");
            return;
        }

        try {
            // Determine topic name based on data source name
            String topicName = BWDataTopicNameUtil.getTopicName(dataRequest);

            // Serialize BW data request to JSON
            String requestJson = objectMapper.writeValueAsString(dataRequest);

            // Create Kafka producer record
            ProducerRecord<String, String> record = new ProducerRecord<>(
                topicName,
                dataRequest.getId(),  // Use request ID as key
                requestJson
            );

            // Send synchronously with timeout
            kafkaProducer.send(record).get();

            LOGGER.info("Successfully published BW data request {} to topic {}",
                dataRequest.getId(), topicName);

        } catch (Exception e) {
            String errorMsg = String.format("Failed to publish BW data request %s: %s",
                dataRequest.getId(), e.getMessage());
            LOGGER.error(errorMsg, e);
            throw new KafkaPublishException(errorMsg, e);
        }
    }
}

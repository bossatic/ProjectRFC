package org.dataingest.rfc.server.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.talend.sap.idoc.ISAPIDoc;
import org.talend.sap.idoc.ISAPIDocPackage;
import org.dataingest.rfc.server.exception.KafkaPublishException;
import org.dataingest.rfc.server.util.IDocTopicNameUtil;

/**
 * Publisher for SAP IDOC data to Kafka topics.
 *
 * Handles synchronous publishing of IDOC packages directly to Kafka with proper error handling
 * and transaction rollback support.
 */
@Component
public class IDocKafkaPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDocKafkaPublisher.class);

    @Autowired
    protected Producer<String, String> kafkaProducer;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${kafka.request.timeout.ms:30000}")
    private int kafkaTimeoutMs;

    /**
     * Publishes an IDOC package to Kafka.
     *
     * Each IDOC in the package is serialized to JSON and published to a topic based on the
     * IDOC type and extension. Publishes are synchronous to ensure delivery before transaction
     * commit.
     *
     * @param idocPackage the IDOC package to publish
     * @throws KafkaPublishException if publishing fails
     */
    public void publish(ISAPIDocPackage idocPackage) throws KafkaPublishException {
        if (idocPackage == null || idocPackage.getIDocs().isEmpty()) {
            LOGGER.debug("Skipping empty IDOC package");
            return;
        }

        try {
            for (ISAPIDoc idoc : idocPackage.getIDocs()) {
                publishIdoc(idoc);
            }

            LOGGER.info("Successfully published {} IDOCs from package {}",
                idocPackage.getIDocs().size(), idocPackage.getTransactionId());

        } catch (Exception e) {
            String errorMsg = String.format("Failed to publish IDOC package %s: %s",
                idocPackage.getTransactionId(), e.getMessage());
            LOGGER.error(errorMsg, e);
            throw new KafkaPublishException(errorMsg, e);
        }
    }

    /**
     * Publishes a single IDOC to Kafka.
     *
     * @param idoc the IDOC to publish
     * @throws Exception if serialization or publishing fails
     */
    private void publishIdoc(ISAPIDoc idoc) throws Exception {
        // Determine topic name based on IDOC type and extension
        String topicName = IDocTopicNameUtil.getTopicName(idoc);

        // Serialize IDOC to JSON
        String idocJson = objectMapper.writeValueAsString(idoc);

        // Create Kafka producer record
        ProducerRecord<String, String> record = new ProducerRecord<>(
            topicName,
            idoc.getDocNum(),  // Use IDOC document number as key
            idocJson
        );

        // Send synchronously with timeout
        try {
            kafkaProducer.send(record).get();
            LOGGER.debug("Published IDOC {} to topic {}", idoc.getDocNum(), topicName);
        } catch (Exception e) {
            LOGGER.error("Failed to publish IDOC {} to topic {}: {}",
                idoc.getDocNum(), topicName, e.getMessage(), e);
            throw e;
        }
    }
}

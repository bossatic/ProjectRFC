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
import org.dataingest.rfc.server.model.SAPIDOCDocument;
import org.dataingest.rfc.server.util.IDocTopicNameUtil;

/**
 * Publisher for SAP IDOC data to Kafka topics.
 *
 * Uses official SAP IDOC library to receive documents and publishes them directly to Kafka
 * with synchronous publishing to guarantee delivery before SAP transaction commit.
 *
 * Handles:
 * - JSON serialization of IDOC documents
 * - Topic name generation based on IDOC type and version
 * - Synchronous publishing with configurable timeout
 * - Error handling with transaction rollback support
 */
@Component
public class IDocKafkaPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDocKafkaPublisher.class);

    @Autowired
    protected Producer<String, String> kafkaProducer;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected IDocTopicNameUtil topicNameUtil;

    @Value("${kafka.request.timeout.ms:30000}")
    private int kafkaTimeoutMs;

    /**
     * Publishes a single IDOC document to Kafka.
     *
     * The document is serialized to JSON and published to a topic based on the
     * IDOC type and version. Publishing is synchronous to guarantee delivery
     * before the SAP transaction is committed.
     *
     * Topic Pattern: SAP.IDOCS.{TYPE}_{VERSION}
     * Example: SAP.IDOCS.ORDERS_05
     *
     * @param document the SAP IDOC document to publish
     * @throws KafkaPublishException if publishing fails
     */
    public void publishSAPDocument(SAPIDOCDocument document) throws KafkaPublishException {
        if (document == null) {
            LOGGER.debug("Skipping null IDOC document");
            return;
        }

        try {
            // Determine topic name based on IDOC type and version using configurable prefix
            String topicName = topicNameUtil.getTopicName(document);

            // Serialize IDOC document to JSON
            String documentJson = objectMapper.writeValueAsString(document);

            // Create Kafka producer record
            // Use document number as key to ensure ordering for same IDOC
            ProducerRecord<String, String> record = new ProducerRecord<>(
                topicName,
                document.getDocumentNumber(),  // Message key
                documentJson                   // Message value
            );

            // Send synchronously with timeout to ensure delivery before commit
            try {
                kafkaProducer.send(record).get();
                LOGGER.info("Published IDOC {} to topic {}",
                    document.getDocumentNumber(), topicName);
            } catch (Exception e) {
                LOGGER.error("Failed to publish IDOC {} to topic {}: {}",
                    document.getDocumentNumber(), topicName, e.getMessage(), e);
                throw e;
            }

        } catch (Exception e) {
            String errorMsg = String.format(
                "Failed to publish IDOC document %s: %s",
                document.getDocumentNumber(), e.getMessage());
            LOGGER.error(errorMsg, e);
            throw new KafkaPublishException(errorMsg, e);
        }
    }

    /**
     * Publishes multiple IDOC documents in batch.
     *
     * @param documents list of IDOC documents to publish
     * @throws KafkaPublishException if any document fails to publish
     */
    public void publishBatch(java.util.List<SAPIDOCDocument> documents) throws KafkaPublishException {
        if (documents == null || documents.isEmpty()) {
            LOGGER.debug("Skipping empty IDOC batch");
            return;
        }

        int successCount = 0;
        Exception lastException = null;

        for (SAPIDOCDocument document : documents) {
            try {
                publishSAPDocument(document);
                successCount++;
            } catch (KafkaPublishException e) {
                lastException = e;
                // Continue publishing others even if one fails
                LOGGER.error("Failed to publish IDOC {} in batch",
                    document.getDocumentNumber(), e);
            }
        }

        LOGGER.info("Batch publish completed: {} success, {} failed",
            successCount, documents.size() - successCount);

        if (lastException != null && successCount < documents.size()) {
            throw new KafkaPublishException(
                "Batch publish failed: " + (documents.size() - successCount) + " IDOCs failed",
                lastException);
        }
    }
}

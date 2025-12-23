package org.dataingest.rfc.server.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.sap.idoc.ISAPIDocPackage;
import org.talend.sap.idoc.ISAPIDocReceiver;
import org.dataingest.rfc.server.exception.KafkaPublishException;
import org.dataingest.rfc.server.publisher.IDocKafkaPublisher;

/**
 * Adapter for wrapping external IDOC receivers to support direct Kafka publishing.
 *
 * This adapter bridges the gap between the SAP RFC receiver interface and the new
 * Kafka publishing mechanism, handling transaction commit/rollback accordingly.
 */
public class IDocReceiverAdapter implements ISAPIDocReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDocReceiverAdapter.class);

    private final ISAPIDocReceiver wrappedReceiver;
    private final IDocKafkaPublisher kafkaPublisher;

    /**
     * Constructs an adapter wrapping an existing IDOC receiver.
     *
     * @param wrappedReceiver the underlying SAP IDOC receiver
     * @param kafkaPublisher  the Kafka publisher for IDOC data
     */
    public IDocReceiverAdapter(ISAPIDocReceiver wrappedReceiver, IDocKafkaPublisher kafkaPublisher) {
        this.wrappedReceiver = wrappedReceiver;
        this.kafkaPublisher = kafkaPublisher;
    }

    /**
     * Receives an IDOC package from SAP and publishes it to Kafka.
     *
     * Flow:
     * 1. Receive IDOC package from SAP RFC call
     * 2. Publish to Kafka synchronously
     * 3. On success: commit SAP transaction (via idocPackage.commit())
     * 4. On failure: rollback SAP transaction (via idocPackage.rollback())
     *
     * @param idocPackage the IDOC package received from SAP
     * @throws Exception if transaction handling fails
     */
    @Override
    public void receiveIdoc(ISAPIDocPackage idocPackage) throws Exception {
        String transactionId = idocPackage.getTransactionId();

        try {
            // Publish IDOC package to Kafka
            kafkaPublisher.publish(idocPackage);

            // Commit SAP transaction
            idocPackage.commit();

            LOGGER.info("IDOC package {} processed successfully - transaction committed", transactionId);

        } catch (KafkaPublishException e) {
            // Handle Kafka publishing failure
            String errorMsg = "Failed to publish IDOC package: " + e.getMessage();
            LOGGER.error(errorMsg, e);

            try {
                // Rollback SAP transaction
                idocPackage.rollback(errorMsg);
                LOGGER.info("IDOC package {} rolled back due to Kafka publishing failure", transactionId);
            } catch (Exception rollbackError) {
                LOGGER.error("Failed to rollback IDOC package {}: {}", transactionId, rollbackError.getMessage());
                throw rollbackError;
            }

            throw e;

        } catch (Exception e) {
            // Handle unexpected errors
            String errorMsg = "Unexpected error processing IDOC package: " + e.getMessage();
            LOGGER.error(errorMsg, e);

            try {
                idocPackage.rollback(errorMsg);
            } catch (Exception rollbackError) {
                LOGGER.error("Failed to rollback IDOC package {}: {}", transactionId, rollbackError.getMessage());
            }

            throw e;
        }
    }
}

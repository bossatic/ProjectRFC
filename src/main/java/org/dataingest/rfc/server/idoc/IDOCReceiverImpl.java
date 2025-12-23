package org.dataingest.rfc.server.idoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.dataingest.rfc.server.model.SAPIDOCDocument;
import org.dataingest.rfc.server.publisher.IDocKafkaPublisher;
import org.dataingest.rfc.server.exception.KafkaPublishException;
import java.util.List;

/**
 * Implementation of IIDOCReceiver using SAP JCo.
 *
 * Receives IDOC packages from SAP and publishes them to Kafka.
 * Handles transaction management using SAP tRFC (transactional RFC).
 */
@Component
public class IDOCReceiverImpl implements IIDOCReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDOCReceiverImpl.class);

    private static final String RECEIVER_NAME = "TALEND";

    @Autowired
    private IDocKafkaPublisher idocPublisher;

    /**
     * Receives an IDOC package from SAP and publishes to Kafka.
     *
     * Flow:
     * 1. Extract IDOCs from the package
     * 2. Publish each IDOC to Kafka
     * 3. On success: commit the SAP transaction
     * 4. On failure: rollback the SAP transaction
     *
     * @param idocPackage the IDOC package from SAP
     * @throws Exception if processing fails
     */
    @Override
    public void receiveIdoc(IIDOCPackage idocPackage) throws Exception {
        String tid = idocPackage.getTID();
        String partnerHost = idocPackage.getPartnerHost();
        int packageSize = idocPackage.size();

        LOGGER.info("╔═══════════════════════════════════════════════════════════╗");
        LOGGER.info("║         IDOC RECEIVER - Processing IDOC Package           ║");
        LOGGER.info("╚═══════════════════════════════════════════════════════════╝");
        LOGGER.info("Partner Host: {}", partnerHost);
        LOGGER.info("Transaction ID (TID): {}", tid);
        LOGGER.info("Package Size: {} IDOC(s)", packageSize);

        if (packageSize == 0) {
            LOGGER.warn("⚠️  Received empty IDOC package");
            idocPackage.commit();
            return;
        }

        int successCount = 0;
        int failureCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        try {
            LOGGER.info("→ Starting Kafka publishing for {} IDOC(s)...", packageSize);

            for (int i = 0; i < packageSize; i++) {
                SAPIDOCDocument idoc = idocPackage.get(i);
                try {
                    LOGGER.info("  [{}/{}] Publishing IDOC: docNum={}, type={}, version={}",
                            (i + 1), packageSize,
                            idoc.getDocumentNumber(),
                            idoc.getMessageType(),
                            idoc.getMessageTypeVersion());
                    LOGGER.debug("    Topic: {}", idoc.getTopicName());
                    LOGGER.debug("    Segments: {}", idoc.getSegmentData().size());

                    idocPublisher.publishSAPDocument(idoc);

                    LOGGER.info("    ✓ Successfully published IDOC {} to Kafka topic: {}",
                            idoc.getDocumentNumber(), idoc.getTopicName());
                    successCount++;

                } catch (KafkaPublishException e) {
                    failureCount++;
                    String errorMsg = String.format("IDOC %s publish error: %s",
                            idoc.getDocumentNumber(), e.getMessage());
                    LOGGER.error("    ✗ FAILED to publish IDOC {}: {}", idoc.getDocumentNumber(), e.getMessage(), e);
                    errorMessages.append(errorMsg).append("; ");
                } catch (Exception e) {
                    failureCount++;
                    String errorMsg = String.format("IDOC %s unexpected error: %s",
                            idoc.getDocumentNumber(), e.getMessage());
                    LOGGER.error("    ✗ Unexpected error publishing IDOC {}: {}",
                            idoc.getDocumentNumber(), e.getMessage(), e);
                    errorMessages.append(errorMsg).append("; ");
                }
            }

            LOGGER.info("← Kafka publishing completed. Success: {}/{}, Failed: {}/{}",
                    successCount, packageSize, failureCount, packageSize);

            // Check if all IDOCs were published successfully
            if (failureCount == 0) {
                LOGGER.info("✓ All IDOCs published successfully. Committing transaction TID: {}", tid);
                idocPackage.commit();
            } else if (successCount > 0) {
                // Partial success - commit anyway (partial delivery)
                LOGGER.warn("⚠️  Partial success: {}/{} IDOCs published. Committing transaction TID: {}",
                        successCount, packageSize, tid);
                idocPackage.commit();
            } else {
                // Complete failure - rollback
                String rollbackReason = "All IDOCs failed to publish: " + errorMessages.toString();
                LOGGER.error("✗ Complete failure: 0/{} IDOCs published. Rolling back transaction TID: {}",
                        packageSize, tid);
                idocPackage.rollback(rollbackReason);
                throw new Exception("Failed to publish any IDOCs from package " + tid);
            }

        } catch (Exception e) {
            LOGGER.error("╔═══════════════════════════════════════════════════════════╗");
            LOGGER.error("║         ERROR PROCESSING IDOC PACKAGE                     ║");
            LOGGER.error("╚═══════════════════════════════════════════════════════════╝");
            LOGGER.error("TID: {}", tid);
            LOGGER.error("Error: {}", e.getMessage(), e);

            try {
                idocPackage.rollback("Error during IDOC processing: " + e.getMessage());
            } catch (Exception rollbackError) {
                LOGGER.error("✗ Error rolling back transaction: {}", rollbackError.getMessage());
            }

            throw new Exception("Failed to process IDOC package " + tid, e);
        }
    }

    @Override
    public String getName() {
        return RECEIVER_NAME;
    }

    @Override
    public boolean isTransactional() {
        return true;  // IDOCs use tRFC, which is transactional
    }
}

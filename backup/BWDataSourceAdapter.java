package org.dataingest.rfc.server.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.sap.bw.ISAPBWDataRequest;
import org.talend.sap.bw.ISAPBWSourceSystem;
import org.dataingest.rfc.server.exception.KafkaPublishException;
import org.dataingest.rfc.server.publisher.BWDataKafkaPublisher;

/**
 * Adapter for wrapping external BW source systems to support direct Kafka publishing.
 *
 * This adapter bridges the gap between the SAP BW source system interface and the new
 * Kafka publishing mechanism, handling errors appropriately.
 */
public class BWDataSourceAdapter implements ISAPBWSourceSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(BWDataSourceAdapter.class);

    private final ISAPBWSourceSystem wrappedSourceSystem;
    private final BWDataKafkaPublisher kafkaPublisher;

    /**
     * Constructs an adapter wrapping an existing BW source system.
     *
     * @param wrappedSourceSystem the underlying SAP BW source system
     * @param kafkaPublisher      the Kafka publisher for BW data
     */
    public BWDataSourceAdapter(ISAPBWSourceSystem wrappedSourceSystem, BWDataKafkaPublisher kafkaPublisher) {
        this.wrappedSourceSystem = wrappedSourceSystem;
        this.kafkaPublisher = kafkaPublisher;
    }

    /**
     * Receives a BW data request and publishes it to Kafka.
     *
     * Flow:
     * 1. Receive BW data request from SAP BW system
     * 2. Publish to Kafka synchronously
     * 3. On success: return normally
     * 4. On failure: throw exception to signal error to SAP
     *
     * @param dataRequest the BW data request received from SAP
     * @throws Exception if publishing fails or data request is invalid
     */
    @Override
    public void receiveData(ISAPBWDataRequest dataRequest) throws Exception {
        String requestId = dataRequest.getId();

        try {
            // Publish BW data request to Kafka
            kafkaPublisher.publish(dataRequest);

            LOGGER.info("BW data request {} published successfully to Kafka", requestId);

        } catch (KafkaPublishException e) {
            // Handle Kafka publishing failure
            String errorMsg = "Failed to publish BW data request " + requestId + ": " + e.getMessage();
            LOGGER.error(errorMsg, e);

            throw e;

        } catch (Exception e) {
            // Handle unexpected errors
            String errorMsg = "Unexpected error publishing BW data request " + requestId + ": " + e.getMessage();
            LOGGER.error(errorMsg, e);

            throw e;
        }
    }
}

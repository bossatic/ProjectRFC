package org.dataingest.rfc.server.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.sap.bw.ISAPBWSourceSystem;
import org.talend.sap.bw.SAPBWSourceSystemFactory;
import org.dataingest.rfc.server.publisher.BWDataKafkaPublisher;

/**
 * Factory for creating BW Source System instances.
 *
 * Injects the BWDataKafkaPublisher to enable direct Kafka publishing instead of blocking queues.
 * Removes all mock functionality.
 */
@Component
public class BWDataSourceFactoryImpl implements SAPBWSourceSystemFactory {

    @Autowired
    protected BWDataKafkaPublisher kafkaPublisher;

    /**
     * Creates a BW source system that publishes directly to Kafka.
     *
     * The source system is configured to use synchronous Kafka publishing.
     *
     * @return the BW source system instance
     */
    @Override
    public ISAPBWSourceSystem create() {
        // Note: Implementation depends on whether ISAPBWSourceSystem
        // is external (from sap-impl JAR) or internal class.
        //
        // If external: Use adapter pattern (BWDataSourceAdapter)
        // If internal: Modify to inject kafkaPublisher directly
        //
        // This is a placeholder for the actual implementation.
        // The adapter or modified source system should:
        // 1. Accept ISAPBWDataRequest from SAP BW system
        // 2. Call kafkaPublisher.publish(dataRequest)
        // 3. If successful: return normally
        // 4. If failed: throw exception to signal error to SAP

        throw new UnsupportedOperationException(
            "Implementation depends on whether sap-impl classes are external. " +
                "Use adapter pattern or modify existing source system implementation."
        );
    }
}

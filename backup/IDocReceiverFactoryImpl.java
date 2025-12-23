package org.dataingest.rfc.server.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.sap.idoc.ISAPIDocReceiver;
import org.talend.sap.server.SAPIDocReceiverFactory;
import org.dataingest.rfc.server.publisher.IDocKafkaPublisher;

/**
 * Factory for creating IDOC Receiver instances.
 *
 * Injects the IDocKafkaPublisher to enable direct Kafka publishing instead of blocking queues.
 * Removes all mock functionality.
 */
@Component
public class IDocReceiverFactoryImpl implements SAPIDocReceiverFactory {

    @Autowired
    protected IDocKafkaPublisher kafkaPublisher;

    /**
     * Creates an IDOC receiver that publishes directly to Kafka.
     *
     * The receiver is configured to use synchronous Kafka publishing.
     *
     * @return the IDOC receiver instance
     */
    @Override
    public ISAPIDocReceiver create() {
        // Note: Implementation depends on whether ISAPIDocReceiver and ISAPIDocTransaction
        // are external (from sap-impl JAR) or internal classes.
        //
        // If external: Use adapter pattern (IDocReceiverAdapter)
        // If internal: Modify to inject kafkaPublisher directly
        //
        // This is a placeholder for the actual implementation.
        // The adapter or modified receiver should:
        // 1. Accept ISAPIDocPackage from SAP RFC call
        // 2. Call kafkaPublisher.publish(idocPackage)
        // 3. If successful: call idocPackage.commit()
        // 4. If failed: call idocPackage.rollback(errorMsg)

        throw new UnsupportedOperationException(
            "Implementation depends on whether sap-impl classes are external. " +
                "Use adapter pattern or modify existing receiver implementation."
        );
    }
}

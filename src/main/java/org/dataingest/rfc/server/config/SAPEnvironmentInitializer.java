package org.dataingest.rfc.server.config;

import com.sap.conn.jco.ext.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

/**
 * Initializes the SAP JCo environment on application startup.
 *
 * Registers both ServerDataProvider and ClientDataProvider with the SAP JCo
 * Environment so that RFC server and client configuration can be provided at runtime.
 */
@Component
public class SAPEnvironmentInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAPEnvironmentInitializer.class);

    @Autowired
    private SAPServerDataProvider serverDataProvider;

    /**
     * Initializes the SAP JCo environment.
     *
     * This method runs automatically on application startup. Registers ServerDataProvider
     * which supplies RFC server configuration with file-based repository.
     */
    @PostConstruct
    public void initializeSAPEnvironment() {
        try {
            LOGGER.info("Initializing SAP JCo environment");

            // Register the server data provider with the SAP environment
            Environment.registerServerDataProvider(serverDataProvider);
            LOGGER.info("SAP ServerDataProvider registered successfully");

        } catch (IllegalStateException e) {
            // Provider already registered - this is OK, can happen if multiple instances
            LOGGER.info("SAP ServerDataProvider already registered: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to initialize SAP JCo environment: {}", e.getMessage(), e);
            throw new RuntimeException("SAP environment initialization failed", e);
        }
    }
}

package org.dataingest.rfc.server.config;

import com.sap.conn.jco.JCo;
import com.sap.conn.jco.ext.Environment;

/**
 * Initializes JCo environment with custom DestinationDataProvider and ServerDataProvider
 * Must be called once before using any JCo functionality
 */
public class JCoEnvironmentInitializer {

    private static boolean initialized = false;

    /**
     * Initialize JCo environment with custom data providers
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }

        try {
            JCoDestinationDataProvider provider = new JCoDestinationDataProvider();
            Environment.registerDestinationDataProvider(provider);
            Environment.registerServerDataProvider(provider);
            initialized = true;
            System.out.println("[JCo] Environment initialized successfully");
        } catch (Exception e) {
            System.err.println("[JCo] Failed to initialize JCo environment");
            e.printStackTrace();
        }
    }
}

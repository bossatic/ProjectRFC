package org.dataingest.rfc.server.idoc;

import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Handler factory for IDOC RFC function calls.
 *
 * Implements JCoServerFunctionHandlerFactory to provide handler instances
 * for incoming IDOC RFC function calls from SAP.
 *
 * This factory returns the IDOCFunctionHandler which bridges JCo's RFC
 * function call mechanism with the custom IIDOCReceiver pattern.
 */
// @Component - DISABLED: Use UnifiedIDOCReceiver instead
// @Primary
public class IDOCHandlerFactory implements JCoServerFunctionHandlerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDOCHandlerFactory.class);

    static {
        System.err.println("!!!!! IDOCHandlerFactory CLASS LOADED !!!!!");
    }

    public IDOCHandlerFactory() {
        System.err.println("!!!!! IDOCHandlerFactory CONSTRUCTOR CALLED !!!!!");
        LOGGER.error("!!!!! IDOCHandlerFactory CONSTRUCTOR CALLED !!!!!");
    }

    @Autowired
    private IDOCFunctionHandler idocFunctionHandler;

    public void afterPropertiesSet() {
        System.err.println("!!!!! IDOCHandlerFactory.afterPropertiesSet() CALLED !!!!!");
        System.err.println("      idocFunctionHandler: " + (idocFunctionHandler != null));
        LOGGER.error("╔═══════════════════════════════════════════════════════════╗");
        LOGGER.error("║       IDOC HANDLER FACTORY - afterPropertiesSet INVOKED    ║");
        LOGGER.error("╚═══════════════════════════════════════════════════════════╝");
        LOGGER.error("idocFunctionHandler initialized: {}", idocFunctionHandler != null);
    }

    /**
     * Returns the IDOC handler for incoming RFC function calls.
     *
     * This method is called by SAP JCo when an RFC function call arrives.
     * It returns the IDOCFunctionHandler which processes IDOC data and
     * passes it to the IIDOCReceiver.
     *
     * @param serverCtx SAP server context for the call
     * @param functionName Name of the RFC function being called
     * @return Handler instance for processing the RFC call
     */
    @Override
    public JCoServerFunctionHandler getCallHandler(JCoServerContext serverCtx, String functionName) {
        System.err.println("═════════════════════════════════════════════════════════════");
        System.err.println("!!!!! getCallHandler() CALLED !!!!!");
        System.err.println("Function Name: " + functionName);
        System.err.println("TID: " + (serverCtx != null ? serverCtx.getTID() : "null"));
        System.err.println("═════════════════════════════════════════════════════════════");

        try {
            LOGGER.info("╔═══════════════════════════════════════════════════════════╗");
            LOGGER.info("║      IDOC HANDLER FACTORY - getCallHandler INVOKED        ║");
            LOGGER.info("╚═══════════════════════════════════════════════════════════╝");
            LOGGER.info("Function Name: {}", functionName);
            LOGGER.info("Server Context: {}", serverCtx);
            LOGGER.info("TID: {}", serverCtx != null ? serverCtx.getTID() : "null");
            LOGGER.info("Thread: {}", Thread.currentThread().getName());

            if (idocFunctionHandler == null) {
                LOGGER.error("ERROR: idocFunctionHandler is NULL!");
                throw new RuntimeException("IDOC handler not initialized");
            }

            // Log all connection attributes for debugging
            try {
                LOGGER.info("Connection Attributes:");
                LOGGER.info("  System ID: {}", serverCtx.getConnectionAttributes().getSystemID());
                LOGGER.info("  User: {}", serverCtx.getConnectionAttributes().getUser());
                LOGGER.info("  Partner Host: {}", serverCtx.getConnectionAttributes().getPartnerHost());
            } catch (Exception e) {
                LOGGER.warn("Could not log attributes: {}", e.getMessage());
            }

            LOGGER.info("✓ Returning IDOC handler for function: {}", functionName);
            System.err.println("!!!!! RETURNING IDOC HANDLER FOR FUNCTION: " + functionName + " !!!!!");
            return idocFunctionHandler;

        } catch (Exception e) {
            System.err.println("!!!!! EXCEPTION IN getCallHandler !!!!!");
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();

            LOGGER.error("✗ Exception in getCallHandler for function {}: {}", functionName, e.getMessage(), e);
            throw new RuntimeException("Failed to get handler for function: " + functionName, e);
        }
    }

    /**
     * Called when an RFC session is closed.
     *
     * @param serverCtx Server context of the closing session
     * @param message Optional message about why the session closed
     * @param error Whether the session closed due to an error
     */
    @Override
    public void sessionClosed(JCoServerContext serverCtx, String message, boolean error) {
        System.err.println("!!!!! sessionClosed() CALLED - error=" + error + ", message=" + message + " !!!!!");
        if (error) {
            LOGGER.error("╔═══════════════════════════════════════════════════════════╗");
            LOGGER.error("║         RFC SESSION CLOSED WITH ERROR                      ║");
            LOGGER.error("╚═══════════════════════════════════════════════════════════╝");
            LOGGER.error("TID: {}", serverCtx != null ? serverCtx.getTID() : "null");
            LOGGER.error("Message: {}", message);
        } else {
            LOGGER.info("╔═══════════════════════════════════════════════════════════╗");
            LOGGER.info("║         RFC SESSION CLOSED SUCCESSFULLY                    ║");
            LOGGER.info("╚═══════════════════════════════════════════════════════════╝");
            LOGGER.info("TID: {}", serverCtx != null ? serverCtx.getTID() : "null");
            LOGGER.info("Message: {}", message);
        }
    }
}

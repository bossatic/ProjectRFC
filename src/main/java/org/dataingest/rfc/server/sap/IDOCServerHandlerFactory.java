package org.dataingest.rfc.server.sap;

import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for creating IDOC RFC function handlers.
 *
 * DEPRECATED: Use org.dataingest.rfc.server.idoc.IDOCHandlerFactory instead.
 * This class is kept for backward compatibility but is not used.
 *
 * Implements JCoServerFunctionHandlerFactory to provide handler instances
 * for incoming RFC function calls from SAP.
 */
// @Component  // DISABLED - Use IDOCHandlerFactory instead
public class IDOCServerHandlerFactory implements JCoServerFunctionHandlerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDOCServerHandlerFactory.class);

    static {
        System.err.println("!!!!! IDOCServerHandlerFactory CLASS LOADED !!!!!");
    }

    @Autowired
    private IDOCServerHandler idocHandler;

    @Autowired
    private DebugRFCHandler debugHandler;

    public IDOCServerHandlerFactory() {
        System.err.println("!!!!! IDOCServerHandlerFactory CONSTRUCTOR CALLED !!!!!");
        LOGGER.error("!!!!! IDOCServerHandlerFactory CONSTRUCTOR CALLED !!!!!");
    }

    public void afterPropertiesSet() {
        System.err.println("!!!!! IDOCServerHandlerFactory.afterPropertiesSet() CALLED !!!!!");
        System.err.println("      idocHandler: " + (idocHandler != null));
        System.err.println("      debugHandler: " + (debugHandler != null));
        LOGGER.error("=== HANDLER FACTORY - afterPropertiesSet INVOKED ===");
        LOGGER.error("idocHandler initialized: {}", idocHandler != null);
        LOGGER.error("debugHandler initialized: {}", debugHandler != null);
    }

    /**
     * Returns the IDOC handler for incoming RFC function calls.
     *
     * @param serverCtx SAP server context for the call
     * @param functionName Name of the RFC function being called
     * @return Handler instance for processing the RFC call
     */
    @Override
    public JCoServerFunctionHandler getCallHandler(JCoServerContext serverCtx, String functionName) {
        System.err.println("===================================================================");
        System.err.println("!!!!! getCallHandler() CALLED !!!!!");
        System.err.println("Function Name: " + functionName);
        System.err.println("TID: " + (serverCtx != null ? serverCtx.getTID() : "null"));
        System.err.println("===================================================================");

        try {
            LOGGER.info("=== HANDLER FACTORY - getCallHandler INVOKED ===");
            LOGGER.info("Function Name: {}", functionName);
            LOGGER.info("Server Context: {}", serverCtx);
            LOGGER.info("TID: {}", serverCtx != null ? serverCtx.getTID() : "null");
            LOGGER.info("Thread: {}", Thread.currentThread().getName());

            if (idocHandler == null) {
                LOGGER.error("ERROR: idocHandler is NULL!");
                throw new RuntimeException("IDOC handler not initialized");
            }

            // Always return the debug handler to capture ANY function calls
            LOGGER.info("✓ Returning DEBUG HANDLER to trace all function calls for: {}", functionName);
            if (debugHandler != null) {
                System.err.println("!!!!! RETURNING DEBUG HANDLER FOR FUNCTION: " + functionName + " !!!!!");
                return debugHandler;
            } else {
                LOGGER.error("  Debug handler is null, returning IDOC handler as fallback");
                return idocHandler;
            }
        } catch (Exception e) {
            System.err.println("!!!!! EXCEPTION IN getCallHandler !!!!!");
            System.err.println("Exception: " + e.getMessage());
            e.printStackTrace();

            LOGGER.error("✗ Exception in getCallHandler for function {}: {}", functionName, e.getMessage(), e);
            throw new RuntimeException("Failed to get handler for function: " + functionName, e);
        }
    }

    /**
     * Called when a session is closed.
     *
     * @param serverCtx Server context of the closing session
     * @param message Optional message about why the session closed
     * @param error Whether the session closed due to an error
     */
    @Override
    public void sessionClosed(JCoServerContext serverCtx, String message, boolean error) {
        System.err.println("!!!!! sessionClosed() CALLED - error=" + error + ", message=" + message);
        if (error) {
            LOGGER.error("=== RFC SESSION CLOSED WITH ERROR ===");
            LOGGER.error("TID: {}", serverCtx != null ? serverCtx.getTID() : "null");
            LOGGER.error("Message: {}", message);
        } else {
            LOGGER.info("=== RFC SESSION CLOSED SUCCESSFULLY ===");
            LOGGER.info("TID: {}", serverCtx != null ? serverCtx.getTID() : "null");
            LOGGER.info("Message: {}", message);
        }
    }
}

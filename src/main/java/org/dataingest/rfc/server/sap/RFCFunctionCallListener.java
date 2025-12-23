package org.dataingest.rfc.server.sap;

import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.AbapException;
import org.dataingest.rfc.server.idoc.UnifiedIDOCReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listener for incoming RFC function calls on the server.
 *
 * For tRFC IDOCs, the function handler factory's getCallHandler() might not be called
 * by SAP JCo in the standard way. This listener provides an alternative mechanism to
 * accept and process function calls directly from the server.
 */
@Component
public class RFCFunctionCallListener implements JCoServerFunctionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RFCFunctionCallListener.class);
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Autowired
    private UnifiedIDOCReceiver unifiedIDOCReceiver;

    /**
     * This method is called by SAP JCo when a function call is received on the RFC server.
     *
     * @param serverCtx The server context containing the RFC request
     * @param function The JCo function object with parameters
     */
    @Override
    public void handleRequest(JCoServerContext serverCtx, JCoFunction function) {
        System.err.println("===================================================================");
        System.err.println("!!!!! RFCFunctionCallListener.handleRequest() CALLED");
        System.err.println("Function Name: " + function.getName());
        System.err.println("TID: " + serverCtx.getTID());
        System.err.println("===================================================================");

        LOGGER.error("=== RFC FUNCTION CALL LISTENER - handleRequest INVOKED ===");
        LOGGER.error("Function Name: {}", function.getName());
        LOGGER.error("TID: {}", serverCtx.getTID());

        // Delegate to UnifiedIDOCReceiver to process the actual RFC function
        try {
            unifiedIDOCReceiver.handleRequest(serverCtx, function);
            System.err.println("✓ Successfully processed RFC function call");
            LOGGER.error("✓ Successfully processed RFC function call");
        } catch (AbapException e) {
            System.err.println("✗ ABAP Error processing RFC function call: " + e.getMessage());
            e.printStackTrace();
            LOGGER.error("✗ ABAP Error processing RFC function call: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing RFC function", e);
        } catch (Exception e) {
            System.err.println("✗ Error processing RFC function call: " + e.getMessage());
            e.printStackTrace();
            LOGGER.error("✗ Error processing RFC function call: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing RFC function", e);
        }
    }

    /**
     * Initialize the function listener on the RFC server.
     * This should be called after the server is created.
     *
     * @param rfcServer The SAP JCo RFC server instance
     */
    public void registerWithServer(JCoServer rfcServer) {
        System.err.println("!!!!! Registering RFC Function Call Listener with server");
        LOGGER.error("!!!!! Registering RFC Function Call Listener with server");

        // Note: This method would set the listener if SAP JCo exposes such an API
        // For now, this is handled through the FunctionHandlerFactory registration
    }
}

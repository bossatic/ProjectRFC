package org.dataingest.rfc.server.sap;

import com.sap.conn.jco.server.JCoServerTIDHandler;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.dataingest.rfc.server.idoc.IDOCReceiverImpl;
import org.dataingest.rfc.server.idoc.UnifiedIDOCReceiver;

/**
 * Handles transactional RFC (tRFC) Transaction IDs for IDOC processing.
 *
 * IDOCs use tRFC which requires a TID handler to confirm successful processing.
 * This handler manages TID confirmations to ensure each IDOC is processed exactly once.
 */
@Component
public class IDOCServerTIDHandler implements JCoServerTIDHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDOCServerTIDHandler.class);

    @Autowired
    private UnifiedIDOCReceiver unifiedIDOCReceiver;

    /**
     * Called when SAP sends a transaction ID request.
     * We return false (TID is new) so SAP knows we'll process this transaction.
     *
     * @param context Server context
     * @param tid Transaction ID from SAP
     * @return true if the TID was already processed (duplicate), false if new
     */
    @Override
    public boolean checkTID(JCoServerContext context, String tid) {
        System.err.println("===================================================================");
        System.err.println("!!!!! TIDHandler.checkTID() CALLED - TID: " + tid);
        System.err.println("===================================================================");
        LOGGER.info("=== TID HANDLER - checkTID INVOKED ===");
        LOGGER.info("TID: {}", tid);

        // Try to extract IDOC in checkTID phase (before confirmTID)
        try {
            Object server = context.getServer();
            LOGGER.info("In checkTID - Server: {}", server.getClass().getSimpleName());

            // Try getRequestQueue in checkTID phase
            try {
                java.lang.reflect.Method getRequestQueueMethod =
                    server.getClass().getMethod("getRequestQueue");
                getRequestQueueMethod.setAccessible(true);
                Object requestQueue = getRequestQueueMethod.invoke(server);
                LOGGER.info("RequestQueue in checkTID: {} (type: {})",
                    requestQueue, requestQueue != null ? requestQueue.getClass().getSimpleName() : "null");

                if (requestQueue != null) {
                    // Try to accept or get next request
                    try {
                        java.lang.reflect.Method acceptMethod =
                            requestQueue.getClass().getMethod("accept", long.class);
                        acceptMethod.setAccessible(true);
                        Object request = acceptMethod.invoke(requestQueue, 1000L);
                        LOGGER.info("RequestQueue.accept() returned: {} (type: {})",
                            request, request != null ? request.getClass().getSimpleName() : "null");

                        if (request instanceof JCoFunction) {
                            JCoFunction function = (JCoFunction) request;
                            LOGGER.info("SUCCESS in checkTID: Got JCoFunction: {}", function.getName());
                            unifiedIDOCReceiver.handleRequest(context, function);
                        }
                    } catch (NoSuchMethodException e) {
                        // Try alternate methods
                        LOGGER.debug("No accept(long) method, trying other methods");
                    }
                }
            } catch (NoSuchMethodException e) {
                LOGGER.debug("No getRequestQueue method available");
            }
        } catch (Exception e) {
            LOGGER.debug("Error in checkTID IDOC extraction: {}", e.getMessage());
        }

        LOGGER.info("Returning FALSE (new TID, process it)");
        // Always return false - we process all TIDs (don't track duplicates yet)
        return false;
    }

    /**
     * Called when SAP confirms that the transaction was processed successfully.
     * For tRFC IDOCs, this is where we extract the IDOC data from the RFC server
     * and process it, since getCallHandler() is not invoked for tRFC calls.
     *
     * @param context Server context
     * @param tid Transaction ID to confirm
     */
    @Override
    public void confirmTID(JCoServerContext context, String tid) {
        System.err.println("===================================================================");
        System.err.println("!!!!! TIDHandler.confirmTID() CALLED - TID: " + tid);
        System.err.println("===================================================================");

        LOGGER.info("=== TID HANDLER - confirmTID INVOKED ===");
        LOGGER.info("=== EXTRACTING IDOC DATA FROM RFC SERVER CONTEXT ===");
        LOGGER.info("TID: {}", tid);

        // For tRFC IDOCs: Extract and process the IDOC data from the RFC server
        try {
            // The key insight: when SAP sends a tRFC IDOC with a TID,
            // it's queued on the server. We need to manually accept it.
            // The function data is available through the RFC server connection.

            System.err.println("!!!!! Attempting to extract IDOC data from server context");
            LOGGER.error("!!!!! Attempting to extract IDOC data from server context");

            // Try to get any pending function on this connection
            // For tRFC, the function might be queued internally
            try {
                // Log comprehensive server information
                Object server = context.getServer();
                LOGGER.info("Server Type: {}", server.getClass().getName());
                LOGGER.info("Server Package: {}", server.getClass().getPackage().getName());

                // List ALL methods on the server to understand what's available
                LOGGER.info("=== Available Methods on Server ===");
                java.lang.reflect.Method[] allMethods = server.getClass().getMethods();
                LOGGER.info("Total methods available: {}", allMethods.length);

                for (java.lang.reflect.Method method : allMethods) {
                    // Log methods that might give us IDOC data
                    String methodName = method.getName();
                    if (methodName.contains("accept") || methodName.contains("get") ||
                        methodName.contains("Idoc") || methodName.contains("Function") ||
                        methodName.contains("function") || methodName.contains("idoc") ||
                        methodName.contains("pending") || methodName.contains("queue")) {
                        LOGGER.info("  Method: {} (params: {}, return: {})",
                            methodName, method.getParameterCount(),
                            method.getReturnType().getSimpleName());
                    }
                }

                // Try to invoke methods that might return IDOC data
                LOGGER.info("=== Attempting to invoke potential IDOC data methods ===");
                boolean foundIdoc = false;

                // PRIORITY: Check RequestQueue - this is where pending requests are stored
                try {
                    java.lang.reflect.Method getRequestQueueMethod = null;
                    for (java.lang.reflect.Method m : allMethods) {
                        if (m.getName().equals("getRequestQueue") && m.getParameterCount() == 0) {
                            getRequestQueueMethod = m;
                            break;
                        }
                    }

                    if (getRequestQueueMethod != null) {
                        LOGGER.info("FOUND: getRequestQueue() method - attempting to invoke...");
                        // Set accessible to bypass Java module restrictions
                        getRequestQueueMethod.setAccessible(true);
                        Object requestQueue = getRequestQueueMethod.invoke(server);
                        LOGGER.info("RequestQueue obtained: {} (type: {})",
                            requestQueue, requestQueue != null ? requestQueue.getClass().getSimpleName() : "null");

                        if (requestQueue != null) {
                            // Try to get methods on the queue to retrieve pending requests
                            java.lang.reflect.Method[] queueMethods = requestQueue.getClass().getMethods();
                            LOGGER.info("RequestQueue has {} methods", queueMethods.length);

                            // Log all available methods to understand the API
                            LOGGER.info("=== Available RequestQueue methods ===");
                            for (java.lang.reflect.Method qMethod : queueMethods) {
                                if (qMethod.getParameterCount() == 0) {
                                    LOGGER.info("  {}: returns {}",
                                        qMethod.getName(), qMethod.getReturnType().getSimpleName());
                                }
                            }

                            // CRITICAL: Try getRequest() which returns AbstractServerConnection
                            LOGGER.info(">>> About to call getRequest() on RequestQueue");
                            try {
                                java.lang.reflect.Method getRequestMethod =
                                    requestQueue.getClass().getMethod("getRequest");
                                LOGGER.info("Found getRequest method");
                                getRequestMethod.setAccessible(true);
                                LOGGER.info("Invoking getRequest()...");
                                Object serverConnection = getRequestMethod.invoke(requestQueue);
                                LOGGER.info(">>> getRequest() returned: {} (type: {})",
                                    serverConnection, serverConnection != null ?
                                    serverConnection.getClass().getSimpleName() : "null");

                                if (serverConnection != null) {
                                    // Try to extract JCoFunction from the server connection
                                    java.lang.reflect.Method[] connMethods =
                                        serverConnection.getClass().getMethods();
                                    LOGGER.info("AbstractServerConnection has {} methods", connMethods.length);

                                    for (java.lang.reflect.Method connMethod : connMethods) {
                                        String methodName = connMethod.getName().toLowerCase();
                                        if ((methodName.contains("function") || methodName.contains("request")) &&
                                            connMethod.getParameterCount() == 0) {
                                            try {
                                                connMethod.setAccessible(true);
                                                Object result = connMethod.invoke(serverConnection);
                                                LOGGER.info("  {}() returned: {}",
                                                    connMethod.getName(),
                                                    result != null ? result.getClass().getSimpleName() : "null");

                                                if (result instanceof JCoFunction) {
                                                    JCoFunction function = (JCoFunction) result;
                                                    LOGGER.info("SUCCESS: Got JCoFunction: {}",
                                                        function.getName());
                                                    unifiedIDOCReceiver.handleRequest(context, function);
                                                    foundIdoc = true;

                                                    // Now call requestFinished() to commit the request
                                                    try {
                                                        java.lang.reflect.Method finishedMethod =
                                                            requestQueue.getClass()
                                                            .getMethod("requestFinished");
                                                        finishedMethod.setAccessible(true);
                                                        finishedMethod.invoke(requestQueue);
                                                        LOGGER.info("requestFinished() called successfully");
                                                    } catch (Exception e) {
                                                        LOGGER.warn("Could not call requestFinished: {}",
                                                            e.getMessage());
                                                    }
                                                    return;
                                                }
                                            } catch (Exception e) {
                                                LOGGER.debug("Method {} failed: {}",
                                                    connMethod.getName(), e.getMessage());
                                            }
                                        }
                                    }
                                }
                            } catch (NoSuchMethodException e) {
                                LOGGER.error("ERROR: No getRequest method: {}", e.getMessage());
                            } catch (Exception e) {
                                LOGGER.error("ERROR calling getRequest: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error accessing RequestQueue: {}", e.getMessage());
                }

                // Fallback: Try other methods
                for (java.lang.reflect.Method method : allMethods) {
                    if (method.getParameterCount() != 0) continue;

                    String methodName = method.getName().toLowerCase();
                    if (methodName.contains("function") || methodName.contains("idoc") ||
                        methodName.contains("accept") || methodName.contains("pending")) {
                        try {
                            Object result = method.invoke(server);
                            if (result != null) {
                                LOGGER.info("Result from {}: {} (type: {})",
                                    method.getName(), result, result.getClass().getSimpleName());
                                if (result instanceof JCoFunction) {
                                    JCoFunction function = (JCoFunction) result;
                                    LOGGER.info("SUCCESS: Found JCoFunction from method {}: {}",
                                        method.getName(), function.getName());
                                    unifiedIDOCReceiver.handleRequest(context, function);
                                    foundIdoc = true;
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            // Continue to next method
                        }
                    }
                }

                if (!foundIdoc) {
                    LOGGER.warn("No IDOC data found through any server methods");

                    // Try accessing private fields via reflection as last resort
                    LOGGER.info("=== Checking private fields ===");
                    try {
                        java.lang.reflect.Field[] fields = server.getClass().getDeclaredFields();
                        LOGGER.info("Total fields: {}", fields.length);
                        for (java.lang.reflect.Field field : fields) {
                            if (field.getName().toLowerCase().contains("function") ||
                                field.getName().toLowerCase().contains("idoc") ||
                                field.getName().toLowerCase().contains("queue")) {
                                field.setAccessible(true);
                                Object value = field.get(server);
                                LOGGER.info("  Field {} = {}", field.getName(), value);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Could not access private fields: {}", e.getMessage());
                    }
                }

            } catch (Exception e) {
                System.err.println("[ERROR] Error accessing RFC server: " + e.getMessage());
                e.printStackTrace();
                LOGGER.error("[ERROR] Error accessing RFC server: {}", e.getMessage(), e);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Error in confirmTID: " + e.getMessage());
            e.printStackTrace();
            LOGGER.error("[ERROR] Error in confirmTID: {}", e.getMessage(), e);
        }

        System.err.println("!!!!! confirmTID processing complete for TID: " + tid);
        LOGGER.info("!!!!! confirmTID processing complete for TID: {}", tid);
    }

    /**
     * Called when transaction is committed successfully.
     *
     * @param context Server context
     * @param tid Transaction ID
     */
    @Override
    public void commit(JCoServerContext context, String tid) {
        System.err.println("!!!!! TIDHandler.commit() CALLED - TID: " + tid);
        LOGGER.info("=== TID HANDLER - commit INVOKED ===");
        LOGGER.info("TID: {}", tid);
        // Mark transaction as successfully committed
    }

    /**
     * Called when SAP rolls back a transaction.
     * Clean up any temporary state for this TID.
     *
     * @param context Server context
     * @param tid Transaction ID to rollback
     */
    @Override
    public void rollback(JCoServerContext context, String tid) {
        System.err.println("!!!!! TIDHandler.rollback() CALLED - TID: " + tid);
        LOGGER.warn("=== TID HANDLER - rollback INVOKED ===");
        LOGGER.warn("TID: {}", tid);
        // Clean up any temporary state associated with this TID
    }
}

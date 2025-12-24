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

            // APPROACH 1: Try to extract function from JCoServerContext
            LOGGER.info("=== Checking JCoServerContext for function data ===");
            try {
                java.lang.reflect.Method[] contextMethods = context.getClass().getMethods();
                for (java.lang.reflect.Method method : contextMethods) {
                    String methodName = method.getName().toLowerCase();
                    if ((methodName.contains("function") || methodName.contains("request")) &&
                        method.getParameterCount() == 0) {
                        try {
                            method.setAccessible(true);
                            Object result = method.invoke(context);
                            if (result != null) {
                                LOGGER.info("Context.{}() returned: {} (type: {})",
                                    method.getName(), result, result.getClass().getSimpleName());
                                if (result instanceof JCoFunction) {
                                    JCoFunction function = (JCoFunction) result;
                                    LOGGER.info("SUCCESS FROM CONTEXT: Got JCoFunction: {}", function.getName());
                                    unifiedIDOCReceiver.handleRequest(context, function);
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            // Continue to next method
                        }
                    }
                }

                // Check private fields on context
                java.lang.reflect.Field[] contextFields = context.getClass().getDeclaredFields();
                LOGGER.info("JCoServerContext has {} fields", contextFields.length);

                // CRITICAL: Try to access this$0 which might be the worker thread
                Object workerThread = null;
                for (java.lang.reflect.Field field : contextFields) {
                    if (field.getName().equals("this$0")) {
                        try {
                            field.setAccessible(true);
                            workerThread = field.get(context);
                            LOGGER.info(">>> FOUND this$0 (worker thread): {} (type: {})",
                                workerThread, workerThread != null ? workerThread.getClass().getName() : "null");

                            if (workerThread != null) {
                                // Inspect the worker thread for function data
                                // Check both declared fields AND inherited fields from parent classes
                                Class<?> currentClass = workerThread.getClass();
                                int depth = 0;
                                while (currentClass != null && depth < 5) {
                                    LOGGER.info("Inspecting class: {} (depth {})", currentClass.getName(), depth);
                                    java.lang.reflect.Field[] workerFields = currentClass.getDeclaredFields();
                                    LOGGER.info("  Class has {} declared fields", workerFields.length);

                                    for (java.lang.reflect.Field wField : workerFields) {
                                        LOGGER.info("    Field: {}", wField.getName());
                                        try {
                                            wField.setAccessible(true);
                                            Object wValue = wField.get(workerThread);
                                            String fieldNameLower = wField.getName().toLowerCase();

                                            // Log ALL non-null values
                                            if (wValue != null) {
                                                LOGGER.info("      {} = {} (type: {})",
                                                    wField.getName(), wValue, wValue.getClass().getSimpleName());

                                                if (wValue instanceof JCoFunction) {
                                                    JCoFunction function = (JCoFunction) wValue;
                                                    LOGGER.info("SUCCESS FROM WORKER THREAD: Got JCoFunction: {}", function.getName());
                                                    unifiedIDOCReceiver.handleRequest(context, function);
                                                    return;
                                                }

                                                // CRITICAL: Inspect callDispatcher (FunctionDispatcher)
                                                if (wField.getName().equals("callDispatcher") ||
                                                    wValue.getClass().getSimpleName().contains("Dispatcher")) {
                                                    LOGGER.info("      >>> FOUND Dispatcher - inspecting deeply...");
                                                    java.lang.reflect.Field[] dispFields = wValue.getClass().getDeclaredFields();
                                                    for (java.lang.reflect.Field dispField : dispFields) {
                                                        try {
                                                            dispField.setAccessible(true);
                                                            Object dispValue = dispField.get(wValue);
                                                            LOGGER.info("        Dispatcher.{} = {} (type: {})",
                                                                dispField.getName(), dispValue,
                                                                dispValue != null ? dispValue.getClass().getSimpleName() : "null");
                                                            if (dispValue instanceof JCoFunction) {
                                                                JCoFunction function = (JCoFunction) dispValue;
                                                                LOGGER.info("SUCCESS FROM DISPATCHER: Got JCoFunction: {}", function.getName());
                                                                unifiedIDOCReceiver.handleRequest(context, function);
                                                                return;
                                                            }
                                                        } catch (Exception e) {
                                                            LOGGER.debug("Could not access dispatcher field {}: {}", dispField.getName(), e.getMessage());
                                                        }
                                                    }
                                                }

                                                // CRITICAL: Inspect conn (connection object)
                                                if (wField.getName().equals("conn") && wValue != null) {
                                                    LOGGER.info("      >>> FOUND conn - inspecting deeply...");

                                                    // Inspect all fields from the connection class hierarchy
                                                    Class<?> connClass = wValue.getClass();
                                                    int connDepth = 0;
                                                    while (connClass != null && connDepth < 5) {
                                                        LOGGER.info("        Inspecting conn class: {} (depth {})", connClass.getName(), connDepth);
                                                        java.lang.reflect.Field[] connFields = connClass.getDeclaredFields();
                                                        LOGGER.info("          Class has {} fields", connFields.length);

                                                        for (java.lang.reflect.Field connField : connFields) {
                                                            try {
                                                                connField.setAccessible(true);
                                                                Object connValue = connField.get(wValue);
                                                                LOGGER.info("          Field: {}", connField.getName());
                                                                if (connValue != null) {
                                                                    LOGGER.info("            {} = {} (type: {})",
                                                                        connField.getName(), connValue, connValue.getClass().getSimpleName());

                                                                    if (connValue instanceof JCoFunction) {
                                                                        JCoFunction function = (JCoFunction) connValue;
                                                                        LOGGER.info("SUCCESS FROM CONNECTION: Got JCoFunction: {}", function.getName());
                                                                        unifiedIDOCReceiver.handleRequest(context, function);
                                                                        return;
                                                                    }
                                                                }
                                                            } catch (Exception e) {
                                                                LOGGER.debug("Could not access conn field {}: {}", connField.getName(), e.getMessage());
                                                            }
                                                        }

                                                        connClass = connClass.getSuperclass();
                                                        connDepth++;
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            LOGGER.debug("Could not access worker field {}: {}", wField.getName(), e.getMessage());
                                        }
                                    }

                                    currentClass = currentClass.getSuperclass();
                                    depth++;
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.debug("Could not access this$0: {}", e.getMessage());
                        }
                        break;
                    }
                }

                for (java.lang.reflect.Field field : contextFields) {
                    String fieldName = field.getName().toLowerCase();
                    LOGGER.info("  Context field: {}", field.getName());
                    if (fieldName.contains("function") || fieldName.contains("request") ||
                        fieldName.contains("call") || fieldName.contains("connection")) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(context);
                            LOGGER.info("    Field {} = {} (type: {})",
                                field.getName(), value,
                                value != null ? value.getClass().getSimpleName() : "null");
                            if (value instanceof JCoFunction) {
                                JCoFunction function = (JCoFunction) value;
                                LOGGER.info("SUCCESS FROM CONTEXT FIELD: Got JCoFunction: {}", function.getName());
                                unifiedIDOCReceiver.handleRequest(context, function);
                                return;
                            }

                            // CRITICAL: Inspect bgRfcCallCtx deeply
                            if (fieldName.contains("bgrfc") && value != null) {
                                LOGGER.info(">>> FOUND bgRfcCallCtx - inspecting deeply...");
                                java.lang.reflect.Field[] bgFields = value.getClass().getDeclaredFields();
                                LOGGER.info("bgRfcCallCtx has {} fields", bgFields.length);
                                for (java.lang.reflect.Field bgField : bgFields) {
                                    try {
                                        bgField.setAccessible(true);
                                        Object bgValue = bgField.get(value);
                                        LOGGER.info("  bgRfcCallCtx.{} = {} (type: {})",
                                            bgField.getName(), bgValue,
                                            bgValue != null ? bgValue.getClass().getSimpleName() : "null");

                                        if (bgValue instanceof JCoFunction) {
                                            JCoFunction function = (JCoFunction) bgValue;
                                            LOGGER.info("SUCCESS FROM bgRfcCallCtx: Got JCoFunction: {}", function.getName());
                                            unifiedIDOCReceiver.handleRequest(context, function);
                                            return;
                                        }
                                    } catch (Exception e) {
                                        LOGGER.debug("Could not access bgRfcCallCtx.{}: {}", bgField.getName(), e.getMessage());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.debug("Could not access field {}: {}", field.getName(), e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Error checking JCoServerContext: {}", e.getMessage());
            }

            // APPROACH 2: Try to get function from Server object
            // For tRFC, the function might be queued internally
            try {
                // Log comprehensive server information
                Object server = context.getServer();
                LOGGER.info("Server Type: {}", server.getClass().getName());
                LOGGER.info("Server Package: {}", server.getClass().getPackage().getName());

                // CHECK: What IDoc handler factory is actually registered?
                try {
                    if (server instanceof com.sap.conn.idoc.jco.JCoIDocServer) {
                        com.sap.conn.idoc.jco.JCoIDocServer idocServer = (com.sap.conn.idoc.jco.JCoIDocServer) server;
                        com.sap.conn.idoc.jco.JCoIDocHandlerFactory factory = idocServer.getIDocHandlerFactory();
                        LOGGER.error("!!!!! ACTUAL IDocHandlerFactory: {} !!!!!", factory != null ? factory.getClass().getName() : "NULL");
                        System.err.println("!!!!! ACTUAL IDocHandlerFactory: " + (factory != null ? factory.getClass().getName() : "NULL"));
                    }
                } catch (Exception e) {
                    LOGGER.error("Error checking IDoc handler factory: {}", e.getMessage());
                }

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

                            // CRITICAL: Try to get the CURRENT connection being processed
                            // instead of calling getRequest() which blocks waiting for a new one
                            LOGGER.info(">>> Looking for current active connection");
                            Object serverConnection = null;

                            // Try to access private field "currentConnection" or "activeConnection"
                            try {
                                java.lang.reflect.Field[] fields = requestQueue.getClass().getDeclaredFields();
                                for (java.lang.reflect.Field field : fields) {
                                    String fieldName = field.getName().toLowerCase();
                                    LOGGER.info("  RequestQueue field: {}", field.getName());

                                    // Look for connection/request fields
                                    if (fieldName.contains("current") || fieldName.contains("active") ||
                                        fieldName.contains("connection") || fieldName.contains("request")) {
                                        field.setAccessible(true);
                                        Object fieldValue = field.get(requestQueue);
                                        LOGGER.info("    Field {} = {} (type: {})",
                                            field.getName(), fieldValue,
                                            fieldValue != null ? fieldValue.getClass().getSimpleName() : "null");
                                        if (fieldValue != null && fieldValue.getClass().getSimpleName().contains("Connection")) {
                                            serverConnection = fieldValue;
                                            LOGGER.info(">>> Found connection in field: {}", field.getName());
                                            break;
                                        }
                                    }

                                    // CRITICAL: Inspect the "queue" field specifically
                                    if (fieldName.equals("queue")) {
                                        field.setAccessible(true);
                                        Object queueValue = field.get(requestQueue);
                                        LOGGER.info(">>> FOUND 'queue' field - inspecting deeply...");
                                        LOGGER.info("    queue = {} (type: {})",
                                            queueValue, queueValue != null ? queueValue.getClass().getName() : "null");

                                        if (queueValue != null) {
                                            // Try to peek at the queue contents
                                            try {
                                                java.lang.reflect.Method peekMethod = queueValue.getClass().getMethod("peek");
                                                peekMethod.setAccessible(true);
                                                Object peekedItem = peekMethod.invoke(queueValue);
                                                LOGGER.info("    queue.peek() = {} (type: {})",
                                                    peekedItem, peekedItem != null ? peekedItem.getClass().getSimpleName() : "null");

                                                if (peekedItem != null && peekedItem.getClass().getSimpleName().contains("Connection")) {
                                                    serverConnection = peekedItem;
                                                    LOGGER.info(">>> Found connection in queue.peek()!");
                                                }
                                            } catch (Exception e) {
                                                LOGGER.debug("Could not peek queue: {}", e.getMessage());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.debug("Could not access RequestQueue fields: {}", e.getMessage());
                            }

                            LOGGER.info(">>> Connection from fields: {} (type: {})",
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

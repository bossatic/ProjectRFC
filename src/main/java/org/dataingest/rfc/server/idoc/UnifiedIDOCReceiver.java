package org.dataingest.rfc.server.idoc;

import com.sap.conn.jco.*;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.server.JCoServerFunctionHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.dataingest.rfc.server.model.SAPIDOCDocument;
import org.dataingest.rfc.server.publisher.IDocKafkaPublisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified IDOC Receiver implementing both RFC function handler and IDOC receiver patterns.
 *
 * This class combines the roles that Talend's SAPIDocReceiver handles:
 * 1. Implements JCoServerFunctionHandler - receives RFC function calls from SAP
 * 2. Implements JCoServerFunctionHandlerFactory - creates handler instances for JCoServer
 *
 * When SAP sends IDOC_INBOUND_ASYNCHRONOUS RFC call:
 * 1. SAP JCo invokes getCallHandler() to get a handler instance
 * 2. SAP JCo invokes handleRequest() on the handler with RFC function data
 * 3. We extract IDOC_CONTROL_REC_40 and IDOC_DATA_REC_40 tables
 * 4. We parse the tables into IDOC documents
 * 5. We publish to Kafka
 */
@Component
@Primary
public class UnifiedIDOCReceiver implements JCoServerFunctionHandler, JCoServerFunctionHandlerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedIDOCReceiver.class);

    static {
        System.err.println("!!!!! UnifiedIDOCReceiver CLASS LOADED !!!!!");
    }

    public UnifiedIDOCReceiver() {
        System.err.println("!!!!! UnifiedIDOCReceiver CONSTRUCTOR CALLED !!!!!");
        LOGGER.error("!!!!! UnifiedIDOCReceiver CONSTRUCTOR CALLED !!!!!");
    }

    @Autowired
    private IDocKafkaPublisher idocPublisher;

    /**
     * JCoServerFunctionHandlerFactory method - called when SAP JCo needs a handler.
     * Returns this instance as the handler for all function calls.
     *
     * @param serverCtx SAP server context
     * @param functionName Name of the RFC function being called
     * @return Handler instance (this object itself)
     */
    @Override
    public JCoServerFunctionHandler getCallHandler(JCoServerContext serverCtx, String functionName) {
        System.err.println("═════════════════════════════════════════════════════════════");
        System.err.println("!!!!! getCallHandler() CALLED !!!!!");
        System.err.println("Function Name: " + functionName);
        System.err.println("TID: " + (serverCtx != null ? serverCtx.getTID() : "null"));
        System.err.println("═════════════════════════════════════════════════════════════");

        LOGGER.error("╔═══════════════════════════════════════════════════════════╗");
        LOGGER.error("║    UNIFIED IDOC RECEIVER - getCallHandler INVOKED        ║");
        LOGGER.error("╚═══════════════════════════════════════════════════════════╝");
        LOGGER.error("Function Name: {}", functionName);
        LOGGER.error("Server Context: {}", serverCtx);
        LOGGER.error("TID: {}", serverCtx != null ? serverCtx.getTID() : "null");
        LOGGER.error("Thread: {}", Thread.currentThread().getName());

        return this;
    }

    /**
     * Fallback method to catch any function calls that don't match expected names.
     * This helps debug what functions SAP is actually calling.
     */
    public static class DebugFunctionHandlerFactory implements com.sap.conn.jco.server.JCoServerFunctionHandlerFactory {
        @Override
        public JCoServerFunctionHandler getCallHandler(JCoServerContext ctx, String functionName) {
            System.err.println("!!! DEBUG: Function call received: " + functionName);
            return null;
        }
        @Override
        public void sessionClosed(JCoServerContext ctx, String msg, boolean error) {}
    }

    /**
     * JCoServerFunctionHandler method - called when RFC function is invoked by SAP.
     * This is where IDOC data is received from SAP.
     *
     * @param serverCtx SAP server context with transaction and sender information
     * @param function JCoFunction containing IDOC data in IDOC_CONTROL_REC_40 and IDOC_DATA_REC_40 tables
     */
    @Override
    public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
            throws AbapException, AbapClassException {

        String functionName = "UNKNOWN";
        String tid = "UNKNOWN";
        String senderSystem = "UNKNOWN";
        String partnerHost = "UNKNOWN";

        System.err.println("═════════════════════════════════════════════════════════════");
        System.err.println("!!!!! handleRequest() CALLED !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.err.println("═════════════════════════════════════════════════════════════");

        try {
            functionName = function.getName();
            tid = serverCtx.getTID();
            senderSystem = serverCtx.getConnectionAttributes().getSystemID();
            partnerHost = serverCtx.getConnectionAttributes().getPartnerHost();

            System.err.println("Function Name: " + functionName);
            System.err.println("TID: " + tid);
            System.err.println("Sender System: " + senderSystem);

            LOGGER.error("╔══════════════════════════════════════════════════════════════╗");
            LOGGER.error("║        UNIFIED IDOC RECEIVER - RFC CALL RECEIVED             ║");
            LOGGER.error("╚══════════════════════════════════════════════════════════════╝");
            LOGGER.error("Function Name: {}", functionName);
            LOGGER.info("Function Name: {}", functionName);
            LOGGER.info("SAP System: {}", senderSystem);
            LOGGER.info("Partner Host: {}", partnerHost);
            LOGGER.info("Transaction ID (TID): {}", tid);
            LOGGER.info("Thread: {}", Thread.currentThread().getName());
            LOGGER.info("Timestamp: {}", System.currentTimeMillis());

            // Extract IDOC data from RFC function tables
            LOGGER.info("→ Extracting IDOC data from RFC function tables...");
            List<SAPIDOCDocument> idocs = extractIdocsFromFunction(function, senderSystem);
            LOGGER.info("← IDOC extraction completed. Total IDOCs: {}", idocs.size());

            if (idocs.isEmpty()) {
                LOGGER.warn("⚠️  NO IDOC DATA FOUND in RFC function: {}", functionName);
                return;
            }

            // Publish each IDOC to Kafka
            LOGGER.info("→ Publishing {} IDOC(s) to Kafka...", idocs.size());
            for (SAPIDOCDocument idoc : idocs) {
                try {
                    idocPublisher.publishSAPDocument(idoc);
                    LOGGER.info("  ✓ Published IDOC: {}", idoc.getMessageType());
                } catch (Exception e) {
                    LOGGER.error("  ✗ Failed to publish IDOC: {}", e.getMessage(), e);
                    throw e;
                }
            }
            LOGGER.info("← All IDOCs published to Kafka successfully");

            // Send response back to SAP
            try {
                LOGGER.info("→ Sending response back to SAP...");
                JCoParameterList exports = function.getExportParameterList();
                if (exports != null) {
                    exports.setValue("STATUS", "SUCCESS");
                    exports.setValue("IDOC_COUNT", String.valueOf(idocs.size()));
                    LOGGER.info("  ✓ Response sent: STATUS=SUCCESS, IDOC_COUNT={}", idocs.size());
                } else {
                    LOGGER.warn("  ⚠️  Export parameter list is null, cannot send response");
                }
            } catch (Exception e) {
                LOGGER.warn("  ✗ Could not set export parameters: {}", e.getMessage());
            }

        } catch (Exception e) {
            LOGGER.error("╔══════════════════════════════════════════════════════════════╗");
            LOGGER.error("║            ERROR HANDLING RFC FUNCTION                      ║");
            LOGGER.error("╚══════════════════════════════════════════════════════════════╝");
            LOGGER.error("Function: {}", functionName);
            LOGGER.error("TID: {}", tid);
            LOGGER.error("Error: {}", e.getMessage(), e);

            try {
                JCoParameterList exports = function.getExportParameterList();
                if (exports != null) {
                    exports.setValue("STATUS", "ERROR");
                    exports.setValue("ERROR_MESSAGE", e.getMessage());
                }
            } catch (Exception ex) {
                LOGGER.warn("Could not set error response: {}", ex.getMessage());
            }

            throw new AbapException("IDOC_PROCESSING_ERROR", e.getMessage());
        }
    }

    /**
     * Extracts IDOC documents from RFC function tables.
     *
     * Talend's SAPIDocReceiver extracts from:
     * - IDOC_CONTROL_REC_40 table (contains control records with metadata)
     * - IDOC_DATA_REC_40 table (contains data records with segment information)
     *
     * @param function the RFC function containing IDOC tables
     * @param senderSystem the SAP system ID
     * @return list of extracted IDOC documents
     */
    private List<SAPIDOCDocument> extractIdocsFromFunction(JCoFunction function, String senderSystem) {
        List<SAPIDOCDocument> idocs = new ArrayList<>();

        try {
            JCoParameterList tables = function.getTableParameterList();
            if (tables == null) {
                LOGGER.warn("Table parameter list is null");
                return idocs;
            }

            // Try to extract from IDOC_CONTROL_REC_40 and IDOC_DATA_REC_40 tables (standard IDOC format)
            try {
                JCoTable controlRecordTable = tables.getTable("IDOC_CONTROL_REC_40");
                JCoTable dataRecordTable = tables.getTable("IDOC_DATA_REC_40");

                if (controlRecordTable != null && controlRecordTable.getNumRows() > 0) {
                    LOGGER.info("Found IDOC_CONTROL_REC_40 table with {} rows", controlRecordTable.getNumRows());
                    LOGGER.info("Found IDOC_DATA_REC_40 table with {} rows", dataRecordTable != null ? dataRecordTable.getNumRows() : 0);

                    // Group data records by document number (control record grouping)
                    Map<String, List<String>> documentDataMap = new HashMap<>();
                    if (dataRecordTable != null && dataRecordTable.getNumRows() > 0) {
                        for (int i = 0; i < dataRecordTable.getNumRows(); i++) {
                            dataRecordTable.setRow(i);
                            String documentNumber = dataRecordTable.getString("DOCNUM");
                            String appData = dataRecordTable.getString("SDATA");
                            documentDataMap.computeIfAbsent(documentNumber, k -> new ArrayList<>()).add(appData);
                        }
                    }

                    // Create an IDOC document for each control record
                    for (int i = 0; i < controlRecordTable.getNumRows(); i++) {
                        controlRecordTable.setRow(i);
                        String documentNumber = controlRecordTable.getString("DOCNUM");
                        String messageType = controlRecordTable.getString("MESTYP");

                        SAPIDOCDocument idoc = new SAPIDOCDocument();
                        idoc.setMessageType(messageType);
                        idoc.setMessageTypeVersion(controlRecordTable.getString("MESCOD"));
                        idoc.setSenderSystem(senderSystem);

                        // Add control record as segment
                        StringBuilder controlRecord = new StringBuilder();
                        controlRecord.append("EDI_DC40|");
                        controlRecord.append(controlRecordTable.getString("TABNAM")).append("|");
                        controlRecord.append(controlRecordTable.getString("DOCNUM")).append("|");
                        controlRecord.append(controlRecordTable.getString("MESTYP")).append("|");
                        controlRecord.append(messageType);
                        idoc.addSegment(controlRecord.toString());

                        // Add data records for this document
                        List<String> dataRecords = documentDataMap.getOrDefault(documentNumber, new ArrayList<>());
                        for (String dataRecord : dataRecords) {
                            if (dataRecord != null && !dataRecord.trim().isEmpty()) {
                                idoc.addSegment(dataRecord);
                            }
                        }

                        LOGGER.debug("Created IDOC: type={}, version={}, segments={}, documentNumber={}",
                                idoc.getMessageType(), idoc.getMessageTypeVersion(), dataRecords.size(), documentNumber);

                        idocs.add(idoc);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("IDOC_CONTROL_REC_40/IDOC_DATA_REC_40 tables not found or error: {}", e.getMessage());
            }

            // Log all available table parameters for debugging
            LOGGER.debug("Available table parameters:");
            try {
                for (JCoField field : tables) {
                    LOGGER.debug("  - Table: {}", field.getName());
                }
            } catch (Exception e) {
                LOGGER.debug("Could not iterate table parameters: {}", e.getMessage());
            }

        } catch (Exception e) {
            LOGGER.error("Error extracting IDOCs from function: {}", e.getMessage(), e);
        }

        return idocs;
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

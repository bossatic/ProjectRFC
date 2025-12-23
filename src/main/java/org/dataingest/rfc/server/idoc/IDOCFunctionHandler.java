package org.dataingest.rfc.server.idoc;

import com.sap.conn.jco.*;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.dataingest.rfc.server.model.SAPIDOCDocument;
import java.util.ArrayList;
import java.util.List;

/**
 * JCo Function Handler for receiving IDOCs from SAP.
 *
 * Implements JCoServerFunctionHandler to bridge SAP JCo's RFC function call mechanism
 * with the custom IIDOCReceiver pattern. When SAP sends an IDOC via RFC, this handler:
 *
 * 1. Receives the RFC function call
 * 2. Extracts IDOC data from function parameters
 * 3. Creates an IDOCPackage
 * 4. Passes it to the IIDOCReceiver for processing
 */
// @Component - DISABLED: Use UnifiedIDOCReceiver instead
// @Primary
public class IDOCFunctionHandler implements JCoServerFunctionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDOCFunctionHandler.class);

    static {
        System.err.println("!!!!! IDOCFunctionHandler CLASS LOADED !!!!!");
    }

    public IDOCFunctionHandler() {
        System.err.println("!!!!! IDOCFunctionHandler CONSTRUCTOR CALLED !!!!!");
        LOGGER.error("!!!!! IDOCFunctionHandler CONSTRUCTOR CALLED !!!!!");
    }

    @Autowired
    private IDOCReceiverImpl idocReceiver;

    /**
     * Handles incoming RFC function calls from SAP containing IDOC data.
     *
     * Called by SAP JCo when an RFC function (e.g., IDOC_INBOUND_ASYNCHRONOUS) is invoked.
     * Extracts IDOC data and delegates to the IDOC receiver for processing.
     *
     * @param serverCtx SAP server context with transaction and sender information
     * @param function JCo function object containing IDOC data in parameters
     * @throws AbapException for application-level errors
     * @throws AbapClassException for ABAP class exceptions
     */
    @Override
    public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
            throws AbapException, AbapClassException {

        String functionName = "UNKNOWN";
        String tid = "UNKNOWN";
        String senderSystem = "UNKNOWN";

        try {
            functionName = function.getName();
            tid = serverCtx.getTID();
            senderSystem = serverCtx.getConnectionAttributes().getSystemID();

            LOGGER.info("╔══════════════════════════════════════════════════════════════╗");
            LOGGER.info("║        IDOC FUNCTION HANDLER - RFC CALL RECEIVED             ║");
            LOGGER.info("╚══════════════════════════════════════════════════════════════╝");
            LOGGER.info("Function Name: {}", functionName);
            LOGGER.info("SAP System: {}", senderSystem);
            LOGGER.info("Transaction ID (TID): {}", tid);
            LOGGER.info("Thread: {}", Thread.currentThread().getName());
            LOGGER.info("Timestamp: {}", System.currentTimeMillis());

            // Extract IDOC data from function parameters
            LOGGER.info("→ Extracting IDOC data from function parameters...");
            List<SAPIDOCDocument> idocs = extractIdocsFromFunction(function, senderSystem);
            LOGGER.info("← IDOC extraction completed. Total IDOCs: {}", idocs.size());

            if (idocs.isEmpty()) {
                LOGGER.warn("⚠️  NO IDOC DATA FOUND in RFC function: {}", functionName);
                return;
            }

            // Create IDOC package
            IIDOCPackage idocPackage = new IDOCPackageImpl(tid, senderSystem, idocs);

            // Process through the receiver
            LOGGER.info("→ Passing IDOC package to receiver for processing...");
            idocReceiver.receiveIdoc(idocPackage);
            LOGGER.info("← IDOC package processed successfully");

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
     * Extracts IDOC documents from RFC function parameters.
     *
     * Supports multiple parameter formats:
     * - Single IDOC as a string in import parameter
     * - Multiple IDOCs as lines in a table parameter
     * - IDOC structure in structure parameter
     *
     * @param function the RFC function containing IDOC data
     * @param senderSystem the SAP system ID
     * @return list of extracted IDOC documents
     */
    private List<SAPIDOCDocument> extractIdocsFromFunction(JCoFunction function, String senderSystem) {
        List<SAPIDOCDocument> idocs = new ArrayList<>();

        try {
            JCoParameterList imports = function.getImportParameterList();
            if (imports == null) {
                LOGGER.warn("Import parameter list is null");
                return idocs;
            }

            // Try to extract from IDOC_DATA parameter (common for IDOC_INBOUND_ASYNCHRONOUS)
            try {
                String idocData = imports.getString("IDOC_DATA");
                if (idocData != null && !idocData.trim().isEmpty()) {
                    LOGGER.info("Found IDOC_DATA parameter");
                    SAPIDOCDocument idoc = parseIDOCDocument(idocData, senderSystem);
                    if (idoc != null) {
                        idocs.add(idoc);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("IDOC_DATA parameter not found or error: {}", e.getMessage());
            }

            // Try to extract from IDOC_DATA_TABLE parameter
            try {
                JCoTable table = imports.getTable("IDOC_DATA_TABLE");
                if (table != null && table.getNumRows() > 0) {
                    LOGGER.info("Found IDOC_DATA_TABLE with {} rows", table.getNumRows());
                    for (int i = 0; i < table.getNumRows(); i++) {
                        table.setRow(i);
                        String idocData = table.getString("DATA");
                        if (idocData != null && !idocData.trim().isEmpty()) {
                            SAPIDOCDocument idoc = parseIDOCDocument(idocData, senderSystem);
                            if (idoc != null) {
                                idocs.add(idoc);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("IDOC_DATA_TABLE parameter not found or error: {}", e.getMessage());
            }

            // Log all available parameters for debugging
            LOGGER.debug("Available import parameters:");
            try {
                for (JCoField field : imports) {
                    LOGGER.debug("  - {}: {}", field.getName(), field.getValue());
                }
            } catch (Exception e) {
                LOGGER.debug("Could not iterate parameters: {}", e.getMessage());
            }

        } catch (Exception e) {
            LOGGER.error("Error extracting IDOCs from function: {}", e.getMessage(), e);
        }

        return idocs;
    }

    /**
     * Parses an IDOC document string into a SAPIDOCDocument object.
     *
     * This is a simplified parser. IDOCs have a specific format with:
     * - Control record (EDI_DC40)
     * - Data records (segment lines)
     *
     * @param idocData the IDOC data string
     * @param senderSystem the SAP system ID
     * @return parsed IDOC document
     */
    private SAPIDOCDocument parseIDOCDocument(String idocData, String senderSystem) {
        try {
            // Create a basic IDOC document
            // In a real implementation, you would parse the IDOC format properly
            SAPIDOCDocument idoc = new SAPIDOCDocument();

            // Extract basic info from IDOC header (this is simplified)
            String[] lines = idocData.split("\n");
            if (lines.length > 0) {
                // EDI_DC40 is typically the first segment (control record)
                // For now, create a basic document with the data
                idoc.setMessageType("ORDERS");  // Default, should be parsed from IDOC
                idoc.setMessageTypeVersion("D");
                idoc.setSenderSystem(senderSystem);

                // Add all lines as segments
                for (String line : lines) {
                    if (line != null && !line.trim().isEmpty()) {
                        idoc.addSegment(line);
                    }
                }

                LOGGER.debug("Parsed IDOC: type={}, version={}, segments={}",
                        idoc.getMessageType(), idoc.getMessageTypeVersion(), lines.length);

                return idoc;
            }

        } catch (Exception e) {
            LOGGER.error("Error parsing IDOC document: {}", e.getMessage(), e);
        }

        return null;
    }
}

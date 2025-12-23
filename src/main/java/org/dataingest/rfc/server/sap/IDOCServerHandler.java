package org.dataingest.rfc.server.sap;

import com.sap.conn.jco.*;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.JCoFieldIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.dataingest.rfc.server.model.SAPIDOCDocument;
import org.dataingest.rfc.server.publisher.IDocKafkaPublisher;
import org.dataingest.rfc.server.exception.KafkaPublishException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * RFC Server Function Handler for receiving IDOCs from SAP systems.
 *
 * DEPRECATED: Use org.dataingest.rfc.server.idoc.IDOCFunctionHandler instead.
 * This class is kept for backward compatibility but is not used.
 *
 * Implements JCoServerFunctionHandler to handle incoming RFC function calls from SAP,
 * extracts IDOC data, and publishes to Kafka.
 *
 * Handles IDOCs in various parameter formats:
 * - Single IDOC as a string in import parameter
 * - Multiple IDOCs as lines in a table parameter
 * - IDOC structure in structure parameter
 */
// @Component  // DISABLED - Use IDOCFunctionHandler instead
public class IDOCServerHandler implements JCoServerFunctionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDOCServerHandler.class);

    static {
        System.err.println("!!!!! IDOCServerHandler CLASS LOADED !!!!!");
    }

    public IDOCServerHandler() {
        System.err.println("!!!!! IDOCServerHandler CONSTRUCTOR CALLED !!!!!");
        LOGGER.error("!!!!! IDOCServerHandler CONSTRUCTOR CALLED !!!!!");
    }

    @Autowired
    private IDocKafkaPublisher idocPublisher;

    /**
     * Handles incoming RFC function calls from SAP Gateway.
     *
     * This method is invoked by the SAP JCo server runtime for each incoming RFC call.
     * It extracts IDOC data from the function parameters and publishes to Kafka.
     *
     * @param serverCtx SAP server context with transaction and sender info
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

            LOGGER.info("=== HANDLE REQUEST - RFC FUNCTION RECEIVED ===");
            LOGGER.info("Function Name: {}", functionName);
            LOGGER.info("SAP System: {}", senderSystem);
            LOGGER.info("Transaction ID (TID): {}", tid);
            LOGGER.info("Thread: {}", Thread.currentThread().getName());
            LOGGER.info("Timestamp: {}", System.currentTimeMillis());

            // Extract IDOC data from function parameters
            LOGGER.info("→ Starting IDOC extraction from function parameters...");
            List<SAPIDOCDocument> idocs = extractIdocsFromFunction(function, senderSystem);
            LOGGER.info("← IDOC extraction completed. Total IDOCs extracted: {}", idocs.size());

            if (idocs.isEmpty()) {
                LOGGER.warn("⚠️  NO IDOC DATA FOUND in RFC function: {}", functionName);
                LOGGER.warn("Check if parameters are being received correctly");
                return;
            }

            LOGGER.info("✓ Extracted {} IDOC(s) successfully", idocs.size());

            // Publish each IDOC to Kafka
            LOGGER.info("→ Starting Kafka publishing for {} IDOC(s)...", idocs.size());
            int successCount = 0;
            int failureCount = 0;

            for (int i = 0; i < idocs.size(); i++) {
                SAPIDOCDocument idoc = idocs.get(i);
                try {
                    LOGGER.info("  [{}/{}] Publishing IDOC: docNum={}, type={}, version={}",
                               (i+1), idocs.size(),
                               idoc.getDocumentNumber(),
                               idoc.getMessageType(),
                               idoc.getMessageTypeVersion());
                    LOGGER.debug("    Topic: {}", idoc.getTopicName());
                    LOGGER.debug("    Segments: {}", idoc.getSegmentData().size());

                    idocPublisher.publishSAPDocument(idoc);

                    LOGGER.info("    ✓ Successfully published IDOC {} to Kafka topic: {}",
                               idoc.getDocumentNumber(), idoc.getTopicName());
                    successCount++;
                } catch (KafkaPublishException e) {
                    failureCount++;
                    LOGGER.error("    ✗ FAILED to publish IDOC {}: {}",
                               idoc.getDocumentNumber(), e.getMessage(), e);
                    // Continue with next IDOC even if one fails
                } catch (Exception e) {
                    failureCount++;
                    LOGGER.error("    ✗ Unexpected error publishing IDOC {}: {}",
                               idoc.getDocumentNumber(), e.getMessage(), e);
                }
            }

            LOGGER.info("← Kafka publishing completed. Success: {}, Failed: {}",
                       successCount, failureCount);
            LOGGER.info("✓ Successfully processed RFC function: {} with {}/{} IDOCs published",
                       functionName, successCount, idocs.size());

            // Send response back to SAP
            try {
                LOGGER.info("→ Sending response back to SAP...");
                JCoParameterList exports = function.getExportParameterList();
                if (exports != null) {
                    exports.setValue("STATUS", successCount > 0 ? "SUCCESS" : "PARTIAL");
                    exports.setValue("IDOC_COUNT", String.valueOf(successCount));
                    LOGGER.info("  ✓ Export parameters set: STATUS={}, IDOC_COUNT={}",
                               successCount > 0 ? "SUCCESS" : "PARTIAL", successCount);
                } else {
                    LOGGER.warn("  ⚠️  Export parameter list is null, cannot send response");
                }
            } catch (Exception e) {
                LOGGER.warn("  ✗ Could not set export parameters: {}", e.getMessage(), e);
            }

        } catch (Exception e) {
            LOGGER.error("=== ERROR HANDLING RFC FUNCTION ===");
            LOGGER.error("Function: {}", functionName);
            LOGGER.error("Error Message: {}", e.getMessage());
            LOGGER.error("Stack Trace: ", e);

            // Send error response back to SAP
            try {
                JCoParameterList exports = function.getExportParameterList();
                if (exports != null) {
                    exports.setValue("STATUS", "ERROR");
                    exports.setValue("ERROR_MSG", e.getMessage());
                    LOGGER.info("Error response set in export parameters");
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to set error response: {}", ex.getMessage(), ex);
            }

            // Throw exception to signal failure to SAP
            throw new AbapException("500", "RFC function processing failed: " + e.getMessage());
        }
    }

    /**
     * Extracts IDOC documents from RFC function parameters.
     *
     * Handles multiple parameter formats:
     * 1. String parameter: Direct IDOC text
     * 2. Table parameter: Multiple IDOC lines
     * 3. Structure parameter: IDOC in structure field
     *
     * @param function JCo function containing IDOC parameters
     * @param senderSystem SAP system that sent this RFC call
     * @return List of extracted IDOC documents
     */
    private List<SAPIDOCDocument> extractIdocsFromFunction(JCoFunction function, String senderSystem) {
        List<SAPIDOCDocument> idocs = new ArrayList<>();

        try {
            LOGGER.debug("  extractIdocsFromFunction() called");

            JCoParameterList imports = function.getImportParameterList();
            JCoParameterList tables = function.getTableParameterList();

            LOGGER.info("  📋 Parameter Analysis:");

            // Log all available parameters for debugging
            if (imports != null) {
                LOGGER.info("    Import Parameters:");
                JCoFieldIterator importIter = imports.getFieldIterator();
                boolean hasFields = false;
                int paramCount = 0;
                while (importIter.hasNextField()) {
                    hasFields = true;
                    JCoField field = importIter.nextField();
                    paramCount++;
                    LOGGER.info("      [{}] {} = '{}'",
                               paramCount,
                               field.getName(),
                               field.getValue() != null ? field.getValue().toString().substring(0, Math.min(100, field.getValue().toString().length())) : "null");
                }
                if (!hasFields) {
                    LOGGER.info("      (none)");
                }
            } else {
                LOGGER.warn("    Import parameters: NULL");
            }

            if (tables != null) {
                LOGGER.info("    Table Parameters:");
                JCoFieldIterator tableIter = tables.getFieldIterator();
                boolean hasFields = false;
                int paramCount = 0;
                while (tableIter.hasNextField()) {
                    hasFields = true;
                    JCoField field = tableIter.nextField();
                    paramCount++;
                    LOGGER.info("      [{}] {}", paramCount, field.getName());
                }
                if (!hasFields) {
                    LOGGER.info("      (none)");
                }
            } else {
                LOGGER.warn("    Table parameters: NULL");
            }

            // Format 1: Check for direct IDOC string parameter (try both IDOC and IDOC_DATA)
            LOGGER.info("    → Attempting to extract IDOC_DATA string parameter...");
            String idocData = tryGetStringParameter(imports, "IDOC_DATA");
            if (idocData == null) {
                LOGGER.info("      IDOC_DATA not found, trying IDOC...");
                idocData = tryGetStringParameter(imports, "IDOC");
            }

            if (idocData != null && !idocData.isEmpty()) {
                LOGGER.info("✓ String parameter found!");
                LOGGER.info("=== RECEIVED IDOC DATA ===");
                LOGGER.info("Length: {} characters", idocData.length());
                LOGGER.info("First 500 chars:\n{}", idocData.length() > 500 ? idocData.substring(0, 500) + "...[truncated]" : idocData);
                LOGGER.info("Full content:\n{}", idocData);
                LOGGER.info("=== END IDOC DATA ===");

                LOGGER.info("  → Parsing IDOC data...");
                SAPIDOCDocument idoc = parseIdoc(idocData, senderSystem);
                if (idoc != null) {
                    LOGGER.info("  ✓ IDOC parsed successfully");
                    idocs.add(idoc);
                } else {
                    LOGGER.warn("  ✗ IDOC parsing returned null");
                }
                return idocs;
            } else {
                LOGGER.warn("  ✗ IDOC_DATA/IDOC string parameter not found or empty");
            }

            // Format 2: Check for IDOC in table parameter (line by line)
            if (tables != null) {
                idocData = tryGetTableIdocData(tables);
                if (idocData != null && !idocData.isEmpty()) {
                    SAPIDOCDocument idoc = parseIdoc(idocData, senderSystem);
                    if (idoc != null) {
                        idocs.add(idoc);
                    }
                    return idocs;
                }
            }

            // Format 3: Check for IDOC in structure parameter
            JCoStructure idocStructure = tryGetStructureParameter(imports, "IDOC");
            if (idocStructure != null) {
                idocData = tryGetStructureIdocData(idocStructure);
                if (idocData != null && !idocData.isEmpty()) {
                    SAPIDOCDocument idoc = parseIdoc(idocData, senderSystem);
                    if (idoc != null) {
                        idocs.add(idoc);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("=== ERROR EXTRACTING IDOCS FROM FUNCTION ===");
            LOGGER.error("Error Message: {}", e.getMessage());
            LOGGER.error("Stack Trace: ", e);
        }

        LOGGER.info("  ← Extraction complete. Total IDOCs found: {}", idocs.size());
        return idocs;
    }

    /**
     * Attempts to retrieve a string parameter from parameter list.
     *
     * @param params Parameter list to search
     * @param paramName Name of parameter to retrieve
     * @return Parameter value or null if not found/empty
     */
    private String tryGetStringParameter(JCoParameterList params, String paramName) {
        try {
            if (params != null) {
                String value = params.getString(paramName);
                if (value != null && !value.trim().isEmpty()) {
                    return value;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Parameter {} not found or error retrieving: {}", paramName, e.getMessage());
        }
        return null;
    }

    /**
     * Attempts to retrieve IDOC data from table parameter.
     * Concatenates all lines from the table.
     *
     * @param tables Table parameter list
     * @return Concatenated IDOC data or null if not found
     */
    private String tryGetTableIdocData(JCoParameterList tables) {
        try {
            // Try common table parameter names
            String[] tableNames = {"IDOC_LINES", "IDOC", "DATA", "LINES"};

            for (String tableName : tableNames) {
                try {
                    JCoTable table = tables.getTable(tableName);
                    if (table != null && table.getNumRows() > 0) {
                        StringBuilder sb = new StringBuilder();
                        table.firstRow();

                        do {
                            // Try common line field names
                            String lineData = null;
                            String[] lineFieldNames = {"LINE", "DATA", "IDOC"};

                            for (String fieldName : lineFieldNames) {
                                try {
                                    lineData = table.getString(fieldName);
                                    if (lineData != null && !lineData.isEmpty()) {
                                        break;
                                    }
                                } catch (Exception e) {
                                    // Try next field name
                                }
                            }

                            if (lineData != null) {
                                sb.append(lineData);
                            }
                        } while (table.nextRow());

                        String result = sb.toString();
                        if (!result.isEmpty()) {
                            return result;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("Table {} not found: {}", tableName, e.getMessage());
                    // Try next table name
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error reading IDOC from table: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Attempts to retrieve a structure parameter from parameter list.
     *
     * @param params Parameter list to search
     * @param paramName Name of parameter to retrieve
     * @return Structure or null if not found
     */
    private JCoStructure tryGetStructureParameter(JCoParameterList params, String paramName) {
        try {
            if (params != null) {
                return params.getStructure(paramName);
            }
        } catch (Exception e) {
            LOGGER.debug("Structure {} not found: {}", paramName, e.getMessage());
        }
        return null;
    }

    /**
     * Attempts to retrieve IDOC data from structure parameter.
     *
     * @param structure JCo structure containing IDOC
     * @return IDOC data string or null if not found
     */
    private String tryGetStructureIdocData(JCoStructure structure) {
        try {
            // Try common field names in structure
            String[] fieldNames = {"DATA", "IDOC", "IDOC_DATA", "CONTENT"};

            for (String fieldName : fieldNames) {
                try {
                    String data = structure.getString(fieldName);
                    if (data != null && !data.isEmpty()) {
                        return data;
                    }
                } catch (Exception e) {
                    // Try next field name
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error reading IDOC from structure: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Parses IDOC data and extracts header information.
     *
     * IDOCs contain:
     * - EDI_DC40: Header segment with document number, message type, version
     * - Subsequent segments: Data segments with business content
     *
     * Header format:
     * EDI_DC40[padded doc number][padded version][message type][other fields]...
     *
     * @param idocData Raw IDOC text data
     * @param senderSystem SAP system that sent this IDOC
     * @return Parsed IDOC document or null if parsing fails
     */
    private SAPIDOCDocument parseIdoc(String idocData, String senderSystem) {
        try {
            if (idocData == null || idocData.isEmpty()) {
                return null;
            }

            SAPIDOCDocument doc = new SAPIDOCDocument();
            doc.setSenderSystem(senderSystem);
            doc.setTimestamp(System.currentTimeMillis());

            // Parse IDOC data into segments (split by newline or carriage return)
            String[] lines = idocData.split("[\r\n]+");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    doc.addSegment(line.trim());
                }
            }

            // Parse EDI_DC40 header to extract metadata
            if (idocData.startsWith("EDI_DC40")) {
                parseIdocHeader(idocData, doc);
            } else {
                // If doesn't start with EDI_DC40, find it in the data
                int headerStart = idocData.indexOf("EDI_DC40");
                if (headerStart >= 0) {
                    parseIdocHeader(idocData.substring(headerStart), doc);
                }
            }

            LOGGER.debug("Parsed IDOC: doc={}, type={}, version={}, sender={}",
                        doc.getDocumentNumber(), doc.getMessageType(),
                        doc.getMessageTypeVersion(), doc.getSenderSystem());

            return doc;

        } catch (Exception e) {
            LOGGER.error("Error parsing IDOC: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parses EDI_DC40 header segment to extract IDOC metadata.
     *
     * EDI_DC40 structure (fixed positions):
     * - Positions 0-3: "EDI_DC40" (segment type)
     * - Positions 4-21: Document number (18 chars, padded)
     * - Positions 22-24: Segment number
     * - Positions 40-55: Message type (16 chars)
     * - Positions 56-58: Message type version (3 chars)
     * - And other fields...
     *
     * Note: Actual positions may vary - this extracts intelligently by looking for patterns
     *
     * @param headerLine EDI_DC40 segment line
     * @param doc IDOC document to populate with parsed data
     */
    private void parseIdocHeader(String headerLine, SAPIDOCDocument doc) {
        try {
            // EDI_DC40 is fixed-length format, split by spaces or extract fixed positions
            String[] parts = headerLine.split(" +");

            if (parts.length >= 2) {
                // Typically: EDI_DC40 [docnum] [segnum] [msgtype] [version] ...

                // Document number (usually in position 1, padded)
                String docNum = parts.length > 1 ? parts[1].trim() : "";
                doc.setDocumentNumber(docNum);

                // Message type and version are harder to extract - look for patterns
                // In sample data: "ORDERS02" pattern suggests msgtype + version concatenated
                if (parts.length > 2) {
                    String msgInfo = parts[2];
                    if (msgInfo.length() >= 3) {
                        // Assume last 2 chars are version, rest is message type
                        int lastDigitPos = msgInfo.length() - 2;
                        if (lastDigitPos > 0) {
                            String msgType = msgInfo.substring(0, lastDigitPos);
                            String msgVersion = msgInfo.substring(lastDigitPos);
                            doc.setMessageType(msgType);
                            doc.setMessageTypeVersion(msgVersion);
                        }
                    }
                }
            }

            // Fallback: If we couldn't parse properly, extract from full line
            if (doc.getMessageType() == null || doc.getMessageType().isEmpty()) {
                extractIdocHeaderFromFullLine(headerLine, doc);
            }

        } catch (Exception e) {
            LOGGER.warn("Error parsing EDI_DC40 header: {}", e.getMessage());
            // Use defaults
            if (doc.getMessageType() == null) {
                doc.setMessageType("UNKNOWN");
            }
            if (doc.getMessageTypeVersion() == null) {
                doc.setMessageTypeVersion("00");
            }
            if (doc.getDocumentNumber() == null) {
                doc.setDocumentNumber("0");
            }
        }
    }

    /**
     * Fallback method to extract IDOC header info from full line using pattern matching.
     *
     * @param headerLine Full header line
     * @param doc Document to populate
     */
    private void extractIdocHeaderFromFullLine(String headerLine, SAPIDOCDocument doc) {
        try {
            // Look for pattern like "ORDERS02" (message type + 2-digit version)
            // Message types are typically 5-16 chars, version is 2-3 chars at end

            String workingLine = headerLine.replace("EDI_DC40", "").trim();

            // Try to find message type pattern (uppercase letters)
            for (int i = 0; i < workingLine.length(); i++) {
                if (Character.isLetter(workingLine.charAt(i))) {
                    // Found start of message type
                    int startPos = i;
                    int endPos = i;

                    // Find end of message type (stop at numbers or spaces)
                    while (endPos < workingLine.length() &&
                           Character.isLetter(workingLine.charAt(endPos))) {
                        endPos++;
                    }

                    if (endPos < workingLine.length() &&
                        Character.isDigit(workingLine.charAt(endPos))) {
                        // Found message type
                        String msgType = workingLine.substring(startPos, endPos);
                        doc.setMessageType(msgType);

                        // Get version (next 2-3 digits)
                        int versionEnd = Math.min(endPos + 3, workingLine.length());
                        String version = workingLine.substring(endPos, versionEnd);
                        doc.setMessageTypeVersion(version);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not extract header from full line: {}", e.getMessage());
        }
    }
}

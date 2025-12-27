package org.dataingest.rfc.server.idoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.dataingest.rfc.server.config.IDocCaptureConfig;
import org.dataingest.rfc.server.config.JCoEnvironmentInitializer;
import org.dataingest.rfc.server.kafka.KafkaProducerService;

import com.sap.conn.idoc.IDocDocumentList;
import com.sap.conn.idoc.IDocSegment;
import com.sap.conn.idoc.IDocXMLProcessor;
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.idoc.jco.JCoIDocHandler;
import com.sap.conn.idoc.jco.JCoIDocHandlerFactory;
import com.sap.conn.idoc.jco.JCoIDocServer;
import com.sap.conn.idoc.jco.JCoIDocServerContext;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerTIDHandler;

/**
 * Simple IDoc Capture Program with External Configuration
 *
 * Captures IDocs from SAP and stores them as XML files to a configured directory.
 * Configuration is read from idoc_capture.properties file.
 */
public class SimpleIDocCaptureWithConfig {

    private IDocCaptureConfig config;
    private IDocXMLProcessor xmlProcessor;
    private IDocDocumentationManager docManager;
    private KafkaProducerService kafkaProducer;
    private SimpleDateFormat dateFormat;
    private int idocCount = 0;

    public SimpleIDocCaptureWithConfig(String configFile) throws IOException {
        this.config = new IDocCaptureConfig(configFile);
        this.xmlProcessor = JCoIDoc.getIDocFactory().getIDocXMLProcessor();
        this.docManager = new IDocDocumentationManager(config);
        this.kafkaProducer = new KafkaProducerService(config);
        this.dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    }

    public void start() {
        try {
            // Initialize JCo environment to read .jcoDestination files
            JCoEnvironmentInitializer.init();

            // Validate output directory
            File outputDir = new File(config.getOutputDirectory());
            if (!outputDir.exists()) {
                if (outputDir.mkdirs()) {
                    config.log("Created output directory: " + config.getOutputDirectory());
                } else {
                    throw new IOException("Failed to create output directory: " + config.getOutputDirectory());
                }
            }

            // Get and configure the server
            JCoIDocServer server = JCoIDoc.getServer(config.getJcoServerName());
            server.setIDocHandlerFactory(new MyIDocHandlerFactory());
            server.setTIDHandler(new MyTidHandler());

            ServerErrorListener listener = new ServerErrorListener();
            server.addServerErrorListener(listener);
            server.addServerExceptionListener(listener);

            config.log("Starting IDoc capture server: " + config.getJcoServerName());
            config.log("Output directory: " + config.getOutputDirectory());
            config.log("Listening for IDocs...");

            server.start();

            // Keep the server running
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            config.logError("Failed to start IDoc capture server", e);
        }
    }

    private class MyIDocReceiveHandler implements JCoIDocHandler {
        @Override
        public void handleRequest(JCoServerContext serverCtx, IDocDocumentList idocList) {
            FileOutputStream fos = null;
            OutputStreamWriter osw = null;
            try {
                String tid = serverCtx.getTID();
                String filename = generateFilename(tid);
                String xmlFilePath = config.getOutputDirectory() + File.separator + filename;

                // Write IDoc to XML file
                fos = new FileOutputStream(xmlFilePath);
                osw = new OutputStreamWriter(fos, "UTF-8");
                xmlProcessor.render(idocList, osw, IDocXMLProcessor.RENDER_WITH_TABS_AND_CRLF);
                osw.flush();

                // Extract IDoc type from the XML file we just wrote
                String idocType = extractIdocTypeFromXml(xmlFilePath);

                // Extract document number for Kafka key
                String docNum = extractDocumentNumber(xmlFilePath);

                // Check if documentation exists and JSON conversion is enabled
                if (config.isJsonConversionEnabled() && idocType != null && docManager.hasDocumentation(idocType)) {
                    try {
                        String jsonFilename = filename.replace(".xml", ".json");
                        String jsonFilePath = config.getJsonOutputDirectory() + File.separator + jsonFilename;
                        // Convert XML to JSON asynchronously
                        convertXmlToJsonAsync(xmlFilePath, jsonFilePath, idocType, docNum);
                        config.log("IDoc captured: " + filename + " + " + jsonFilename + " [Type: " + idocType + "] (Total: " + (idocCount + 1) + ")");
                    } catch (Exception jsonError) {
                        config.logError("Failed to schedule JSON conversion (but XML saved): " + idocType, jsonError);
                        config.log("IDoc captured: " + filename + " [Type: " + idocType + "] (Total: " + (idocCount + 1) + ")");
                    }
                } else {
                    if (idocType != null && !docManager.hasDocumentation(idocType)) {
                        config.log("IDoc captured: " + filename + " [Type: " + idocType + " - no documentation] (Total: " + (idocCount + 1) + ")");
                    } else {
                        config.log("IDoc captured: " + filename + " (Total: " + (idocCount + 1) + ")");
                    }
                }

                idocCount++;

            } catch (Exception e) {
                config.logError("Error handling IDoc request", e);
            } finally {
                try {
                    if (osw != null) osw.close();
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    config.logError("Error closing file streams", e);
                }
            }
        }
    }

    /**
     * Extract document number (DOCNUM) from XML
     */
    private String extractDocumentNumber(String xmlFilePath) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.File(xmlFilePath));

            // Try to find DOCNUM element
            org.w3c.dom.NodeList docNumNodes = doc.getElementsByTagName("DOCNUM");
            if (docNumNodes.getLength() > 0) {
                String docNum = docNumNodes.item(0).getTextContent().trim();
                if (!docNum.isEmpty()) {
                    return docNum;
                }
            }
        } catch (Exception e) {
            config.log("Could not extract document number from XML");
        }

        return null;
    }

    /**
     * Extract IDoc type from the XML file
     * Parses the XML to find IDOCTYP element
     */
    private String extractIdocTypeFromXml(String xmlFilePath) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new java.io.File(xmlFilePath));

            // Try to find IDOCTYP element
            org.w3c.dom.NodeList idocTypeNodes = doc.getElementsByTagName("IDOCTYP");
            if (idocTypeNodes.getLength() > 0) {
                String idocType = idocTypeNodes.item(0).getTextContent().trim().toUpperCase();
                config.log("Extracted IDoc type from XML: " + idocType);
                return idocType;
            }

            // Try alternative tag name
            org.w3c.dom.NodeList doctypeNodes = doc.getElementsByTagName("DOCTYP");
            if (doctypeNodes.getLength() > 0) {
                String docType = doctypeNodes.item(0).getTextContent().trim().toUpperCase();
                config.log("Extracted IDoc type from XML (DOCTYP): " + docType);
                return docType;
            }

        } catch (Exception e) {
            config.logError("Error parsing XML to extract IDoc type", e);
        }

        config.log("Could not determine IDoc type from XML");
        return null;
    }

    /**
     * Convert XML to JSON and optionally publish to Kafka (asynchronously)
     */
    private void convertXmlToJsonAsync(String xmlFilePath, String jsonFilePath, String idocType, String docNum) {
        new Thread(() -> {
            try {
                // Convert XML to JSON
                IDocXmlToJsonConverter.convertXmlToJson(xmlFilePath, jsonFilePath);
                config.log("JSON conversion successful for " + idocType);

                // Publish to Kafka if enabled
                if (config.isKafkaPushJson() && kafkaProducer.isInitialized()) {
                    try {
                        String jsonContent = readJsonFile(jsonFilePath);
                        kafkaProducer.publishJson(idocType, docNum, jsonContent);
                    } catch (Exception kafkaError) {
                        config.logError("Error publishing to Kafka for " + idocType, kafkaError);
                    }
                }
            } catch (Exception e) {
                config.logError("Error converting XML to JSON for " + idocType, e);
            }
        }).start();
    }

    /**
     * Read JSON file content
     */
    private String readJsonFile(String jsonFilePath) throws Exception {
        java.nio.file.Path path = java.nio.file.Paths.get(jsonFilePath);
        return new String(java.nio.file.Files.readAllBytes(path), "UTF-8");
    }

    private String generateFilename(String tid) {
        if (config.isUseTimestamp()) {
            String timestamp = dateFormat.format(new Date());
            return "IDOC_" + timestamp + "_" + tid + ".xml";
        } else {
            return tid + "_idoc.xml";
        }
    }

    private class MyIDocHandlerFactory implements JCoIDocHandlerFactory {
        private JCoIDocHandler handler = new MyIDocReceiveHandler();

        @Override
        public JCoIDocHandler getIDocHandler(JCoIDocServerContext serverCtx) {
            return handler;
        }
    }

    private class ServerErrorListener implements JCoServerErrorListener, JCoServerExceptionListener {
        @Override
        public void serverErrorOccurred(com.sap.conn.jco.server.JCoServer server, String connectionId,
                com.sap.conn.jco.server.JCoServerContextInfo ctx, Error error) {
            config.logError("Server error on " + server.getProgramID() + " connection " + connectionId, null);
            error.printStackTrace();
        }

        @Override
        public void serverExceptionOccurred(com.sap.conn.jco.server.JCoServer server, String connectionId,
                com.sap.conn.jco.server.JCoServerContextInfo ctx, Exception error) {
            config.logError("Server exception on " + server.getProgramID() + " connection " + connectionId, error);
        }
    }

    private class MyTidHandler implements JCoServerTIDHandler {
        @Override
        public boolean checkTID(JCoServerContext serverCtx, String tid) {
            config.log("TID check: " + tid);
            return true;
        }

        @Override
        public void confirmTID(JCoServerContext serverCtx, String tid) {
            config.log("TID confirmed: " + tid);
        }

        @Override
        public void commit(JCoServerContext serverCtx, String tid) {
            config.log("Commit: " + tid);
        }

        @Override
        public void rollback(JCoServerContext serverCtx, String tid) {
            config.log("Rollback: " + tid);
        }
    }

    /**
     * Main method - starts the IDoc capture server
     */
    public static void main(String[] args) {
        try {
            String configFile = (args.length > 0) ? args[0] : "idoc_capture.properties";
            SimpleIDocCaptureWithConfig capture = new SimpleIDocCaptureWithConfig(configFile);
            capture.start();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

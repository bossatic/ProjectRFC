package org.dataingest.rfc.server.idoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

import org.dataingest.rfc.server.config.IDocCaptureConfig;
import org.dataingest.rfc.server.config.JCoEnvironmentInitializer;
import org.dataingest.rfc.server.kafka.KafkaProducerService;
import org.dataingest.rfc.server.monitoring.MonitoringManager;
import org.dataingest.rfc.server.monitoring.MetricsStore;
import org.dataingest.rfc.server.monitoring.MonitoringEventPublisher;
import org.dataingest.rfc.server.monitoring.events.*;
import org.dataingest.rfc.server.retention.FileRetentionJob;

import java.time.Instant;

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
    // private IDocDocumentationManager docManager;
    private KafkaProducerService kafkaProducer;
    private SimpleDateFormat dateFormat;
    private int idocCount = 0;
    private MonitoringManager monitoringManager;
    private FileRetentionJob fileRetentionJob;

    // Transaction management - proper tRFC implementation per SAP JCo documentation
    // Track in-progress processing with futures
    private final ConcurrentHashMap<String, CompletableFuture<Void>> tidProcessing = new ConcurrentHashMap<>();
    // Track successfully committed TIDs to prevent duplicates
    private final ConcurrentHashMap<String, Boolean> tidCommitted = new ConcurrentHashMap<>();
    // Thread pool for async processing (but handleRequest will block on completion)
    private final ExecutorService processorExecutor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );

    public SimpleIDocCaptureWithConfig(String configFile) throws IOException {
        this.config = new IDocCaptureConfig(configFile);
        this.xmlProcessor = JCoIDoc.getIDocFactory().getIDocXMLProcessor();
        // this.docManager = new IDocDocumentationManager(config);
        this.kafkaProducer = new KafkaProducerService(config);
        this.dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    }

    public void start() {
        try {
            // Initialize JCo environment to read .jcoDestination files
            JCoEnvironmentInitializer.init();

            // Initialize and start monitoring system
            if (config.isMonitoringEnabled()) {
                try {
                    monitoringManager = new MonitoringManager(config);
                    monitoringManager.initialize();
                    monitoringManager.start();

                    // Pass monitoring components to Kafka producer
                    if (kafkaProducer != null) {
                        kafkaProducer.setMonitoring(
                            monitoringManager.getMetricsStore(),
                            monitoringManager.getEventPublisher()
                        );
                    }
                } catch (Exception e) {
                    config.logError("Failed to start monitoring system (continuing without monitoring)", e);
                }
            }

            // Initialize and start file retention job
            if (config.isFileRetentionEnabled()) {
                try {
                    fileRetentionJob = new FileRetentionJob(config);
                    fileRetentionJob.start();
                } catch (Exception e) {
                    config.logError("Failed to start file retention job (continuing without file cleanup)", e);
                }
            }

            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                config.log("Shutdown signal received, closing resources...");
                processorExecutor.shutdown();
                try {
                    if (!processorExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        processorExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    processorExecutor.shutdownNow();
                }
                if (kafkaProducer != null) {
                    kafkaProducer.close();
                }
                if (monitoringManager != null) {
                    monitoringManager.shutdown();
                }
                if (fileRetentionJob != null) {
                    fileRetentionJob.shutdown();
                }
            }));

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
            String tid = serverCtx.getTID();
            int numDocs = idocList.getNumDocuments();

            config.log("Received IDoc batch with " + numDocs + " IDoc(s), TID: " + tid);

            // Create future for async processing
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    config.log("Processing IDoc batch, TID: " + tid);

                    // Process each IDoc in the batch
                    for (int i = 0; i < numDocs; i++) {
                        processIndividualIDoc(idocList.get(i), tid, i, numDocs);
                    }

                    config.log("Successfully processed IDoc batch, TID: " + tid);
                } catch (Exception e) {
                    config.logError("Failed to process IDoc batch, TID: " + tid, e);
                    throw new RuntimeException("IDoc processing failed for TID " + tid, e);
                }
            }, processorExecutor);

            // Store future for tracking
            tidProcessing.put(tid, future);

            // CRITICAL: Block until processing completes (per tRFC requirements)
            // This ensures SAP only gets success response if processing actually succeeded
            try {
                config.log("Waiting for IDoc processing to complete, TID: " + tid);
                future.get(120, TimeUnit.SECONDS);  // Wait up to 2 minutes
                config.log("IDoc processing completed successfully, TID: " + tid);
            } catch (TimeoutException e) {
                tidProcessing.remove(tid);
                config.logError("IDoc processing timeout for TID: " + tid, e);
                throw new RuntimeException("IDoc processing timeout for TID " + tid, e);
            } catch (Exception e) {
                tidProcessing.remove(tid);
                config.logError("IDoc processing failed for TID: " + tid, e);
                throw new RuntimeException("IDoc processing failed for TID " + tid, e);
            }
        }
    }

    /**
     * Process an individual IDoc from the batch
     */
    private void processIndividualIDoc(com.sap.conn.idoc.IDocDocument idoc,
                                        String tid, int index, int totalInBatch) {
            FileOutputStream fos = null;
            OutputStreamWriter osw = null;
            Instant startTime = Instant.now();
            String idocType = null;
            String docNum = null;
            long xmlSize = 0;

            try {
                // Generate unique filename for this IDoc
                String filename = (totalInBatch > 1) ?
                    generateFilename(tid + "_" + index) :
                    generateFilename(tid);
                String xmlFilePath = config.getOutputDirectory() + File.separator + filename;

                // Write individual IDoc to XML file
                fos = new FileOutputStream(xmlFilePath);
                osw = new OutputStreamWriter(fos, "UTF-8");
                xmlProcessor.render(idoc, osw, IDocXMLProcessor.RENDER_WITH_TABS_AND_CRLF);
                osw.flush();
                osw.close();
                fos.close();

                // Get file size
                File xmlFile = new File(xmlFilePath);
                xmlSize = xmlFile.length();

                // Extract IDoc type from the XML file we just wrote
                idocType = extractIdocTypeFromXml(xmlFilePath);

                // Extract document number for Kafka key
                docNum = extractDocumentNumber(xmlFilePath);

                // Update monitoring - IDoc received and XML written
                if (monitoringManager != null && monitoringManager.isStarted()) {
                    MetricsStore metrics = monitoringManager.getMetricsStore();
                    MonitoringEventPublisher eventPub = monitoringManager.getEventPublisher();

                    metrics.incrementIdocsReceived(idocType);
                    metrics.incrementXmlWritten();
                    metrics.updateLastReceived(idocType, docNum, Instant.now());

                    // Publish IDOC_RECEIVED event
                    IdocReceivedEvent event = new IdocReceivedEvent(idocType, docNum, tid, xmlSize, "SAP");
                    eventPub.publishAsync(event);

                    // Broadcast to SSE clients for dashboard
                    if (monitoringManager.getWebServer() != null &&
                        monitoringManager.getWebServer().getSseServlet() != null) {
                        monitoringManager.getWebServer().getSseServlet()
                            .broadcastIdocReceived(idocType, docNum);
                    }
                }

                // Convert to JSON if enabled
                if (config.isJsonConversionEnabled()) {
                    try {
                        String jsonFilename = filename.replace(".xml", ".json");
                        String jsonFilePath = config.getJsonOutputDirectory() + File.separator + jsonFilename;
                        // Convert XML to JSON synchronously (wait for completion before responding to SAP)
                        convertXmlToJsonSync(xmlFilePath, jsonFilePath, idocType, docNum, startTime, xmlSize);
                        config.log("IDoc captured: " + filename + " + " + jsonFilename + " [Type: " + idocType + "] (Total: " + (idocCount + 1) + ")");
                    } catch (Exception jsonError) {
                        config.logError("Failed to schedule JSON conversion (but XML saved): " + idocType, jsonError);
                        config.log("IDoc captured: " + filename + " [Type: " + idocType + "] (Total: " + (idocCount + 1) + ")");

                        // Publish error event
                        if (monitoringManager != null && monitoringManager.isStarted()) {
                            monitoringManager.getMetricsStore().incrementError(ErrorStage.JSON_CONVERSION);
                            ErrorEvent errorEvent = new ErrorEvent(ErrorStage.JSON_CONVERSION, idocType, docNum,
                                jsonError.getMessage(), jsonError.toString(), true);
                            monitoringManager.getEventPublisher().publishAsync(errorEvent);
                        }
                    }
                } else {
                    config.log("IDoc captured: " + filename + " [Type: " + idocType + "] (Total: " + (idocCount + 1) + ")");
                }

                idocCount++;

            } catch (Exception e) {
                config.logError("Error handling IDoc request", e);

                // Publish error event
                if (monitoringManager != null && monitoringManager.isStarted()) {
                    monitoringManager.getMetricsStore().incrementError(ErrorStage.XML_WRITE);
                    ErrorEvent errorEvent = new ErrorEvent(ErrorStage.XML_WRITE, idocType, docNum,
                        e.getMessage(), e.toString(), false);
                    monitoringManager.getEventPublisher().publishAsync(errorEvent);
                }
            } finally {
                try {
                    if (osw != null) osw.close();
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    config.logError("Error closing file streams", e);
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
     * Convert XML to JSON and optionally publish to Kafka (synchronously)
     * This method blocks until all processing completes to ensure proper tRFC acknowledgment to SAP
     */
    private void convertXmlToJsonSync(String xmlFilePath, String jsonFilePath, String idocType, String docNum,
                                      Instant startTime, long xmlSize) {
        try {
            // Convert XML to JSON
            IDocXmlToJsonConverter.convertXmlToJson(xmlFilePath, jsonFilePath);
            config.log("JSON conversion successful for " + idocType);

            // Get JSON file size
            long jsonSize = new File(jsonFilePath).length();

            // Update monitoring - JSON converted
            if (monitoringManager != null && monitoringManager.isStarted()) {
                MetricsStore metrics = monitoringManager.getMetricsStore();
                MonitoringEventPublisher eventPub = monitoringManager.getEventPublisher();

                metrics.incrementJsonConverted();

                // Publish IDOC_PROCESSED event
                long processingTimeMs = java.time.Duration.between(startTime, Instant.now()).toMillis();
                IdocProcessedEvent event = new IdocProcessedEvent(idocType, docNum, processingTimeMs,
                    xmlSize, jsonSize, java.util.Arrays.asList("XML_WRITTEN", "JSON_CONVERTED"));
                eventPub.publishAsync(event);
            }

            // Publish to Kafka if enabled
            if (config.isKafkaPushJson() && kafkaProducer.isInitialized()) {
                try {
                    String jsonContent = readJsonFile(jsonFilePath);
                    kafkaProducer.publishJson(idocType, docNum, jsonContent);
                } catch (Exception kafkaError) {
                    config.logError("Error publishing to Kafka for " + idocType, kafkaError);

                    // Publish error event
                    if (monitoringManager != null && monitoringManager.isStarted()) {
                        monitoringManager.getMetricsStore().incrementError(ErrorStage.KAFKA_PUBLISH);
                        ErrorEvent errorEvent = new ErrorEvent(ErrorStage.KAFKA_PUBLISH, idocType, docNum,
                            kafkaError.getMessage(), kafkaError.toString(), true);
                        monitoringManager.getEventPublisher().publishAsync(errorEvent);
                    }
                }
            }
        } catch (Exception e) {
            config.logError("Error converting XML to JSON for " + idocType, e);

            // Publish error event
            if (monitoringManager != null && monitoringManager.isStarted()) {
                monitoringManager.getMetricsStore().incrementError(ErrorStage.JSON_CONVERSION);
                ErrorEvent errorEvent = new ErrorEvent(ErrorStage.JSON_CONVERSION, idocType, docNum,
                    e.getMessage(), e.toString(), true);
                monitoringManager.getEventPublisher().publishAsync(errorEvent);
            }
        }
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
        @Override
        public JCoIDocHandler getIDocHandler(JCoIDocServerContext serverCtx) {
            // Create a NEW handler instance for each RFC call to avoid thread safety issues
            // when SAP uses multiple work processes in parallel
            return new MyIDocReceiveHandler();
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
            // Per SAP JCo documentation:
            // - Return TRUE if TID is NEW/valid/not in use (proceed with transaction)
            // - Return FALSE if TID is already COMMITTED (skip duplicate)
            // - If TID is still in execution, WAIT internally before returning

            // Check if already committed
            if (Boolean.TRUE.equals(tidCommitted.get(tid))) {
                config.log("checkTID: " + tid + " -> Already COMMITTED (duplicate) -> Returning FALSE");
                return false;  // Skip, already processed
            }

            // Check if still in execution
            CompletableFuture<Void> future = tidProcessing.get(tid);
            if (future != null) {
                config.log("checkTID: " + tid + " -> Still PROCESSING, waiting for completion...");
                try {
                    // Wait for current processing to complete (per SAP documentation)
                    future.get(120, TimeUnit.SECONDS);
                    config.log("checkTID: " + tid + " -> Processing completed while waiting");

                    // Check again if it was committed
                    if (Boolean.TRUE.equals(tidCommitted.get(tid))) {
                        config.log("checkTID: " + tid + " -> Now COMMITTED -> Returning FALSE");
                        return false;
                    }
                } catch (Exception e) {
                    config.logError("checkTID: " + tid + " -> Processing failed/timeout while waiting", e);
                    // Processing failed, allow retry
                }
            }

            // New TID - proceed with transaction
            config.log("checkTID: " + tid + " -> NEW transaction -> Returning TRUE");
            return true;
        }

        @Override
        public void confirmTID(JCoServerContext serverCtx, String tid) {
            // Per SAP JCo documentation:
            // "This function will be called after the local transaction has been completed.
            //  All resources associated with this TID can be released."
            //
            // At this point, handleRequest has already blocked until processing completed.
            // We just log and clean up the processing future.
            config.log("confirmTID: " + tid + " -> Transaction completed, releasing resources");

            // The future is complete at this point (handleRequest blocked on it)
            // We'll remove it in commit
        }

        @Override
        public void commit(JCoServerContext serverCtx, String tid) {
            // Per SAP JCo documentation:
            // "This function will be called after all RFC functions belonging to a certain
            //  transaction have been successfully completed."
            //
            // Mark this TID as permanently committed so checkTID returns false for duplicates
            config.log("commit: " + tid + " -> Marking as COMMITTED");

            tidCommitted.put(tid, Boolean.TRUE);
            tidProcessing.remove(tid);

            // Clean up old committed TIDs (keep last 10000)
            if (tidCommitted.size() > 10000) {
                config.log("Cleaning up old committed TIDs (keeping last 5000)...");
                tidCommitted.entrySet().stream()
                    .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                    .limit(tidCommitted.size() - 5000)
                    .map(e -> e.getKey())
                    .forEach(tidCommitted::remove);
            }
        }

        @Override
        public void rollback(JCoServerContext serverCtx, String tid) {
            // Per SAP JCo documentation:
            // "This function will be called if an error has occurred in one of the RFC
            //  functions belonging to a certain transaction."
            //
            // Clean up - don't mark as committed, so it can be retried
            config.log("rollback: " + tid + " -> Transaction failed, cleaning up for retry");

            tidProcessing.remove(tid);
            // Don't add to tidCommitted - allow retry
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

package org.dataingest.rfc.server.sap;

import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.JCoFunctionTemplate;
// SAP IDoc API imports
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.idoc.jco.JCoIDocServer;
import com.sap.conn.idoc.jco.JCoIDocHandler;
import com.sap.conn.idoc.jco.JCoIDocHandlerFactory;
import com.sap.conn.idoc.jco.JCoIDocServerContext;
import com.sap.conn.idoc.IDocDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.dataingest.rfc.server.publisher.IDocKafkaPublisher;
import org.dataingest.rfc.server.config.SAPEnvironmentInitializer;
import java.util.Properties;
import javax.annotation.PostConstruct;

/**
 * SAP RFC Server Implementation using SAP JCo (Java Connector) 3.0.
 *
 * This component initializes and manages the RFC Server connection to SAP Gateway.
 * When enabled, it registers with SAP Gateway and listens for incoming IDOC calls,
 * which are then published to Kafka topics.
 *
 * Configuration:
 * - Gateway Host: SAP Gateway hostname/IP where server registers
 * - Gateway Service: SAP Gateway port or service name
 * - Program ID: Unique identifier for this RFC server in SAP
 * - Connection Count: Number of concurrent connections to maintain
 *
 * The IDOC data received from SAP is published to Kafka via IDocKafkaPublisher
 * for downstream consumption.
 */
@Component
public class SAPRFCServerImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAPRFCServerImpl.class);

    // NOT USED - Commenting out to simplify
    // @Autowired
    // private IDocKafkaPublisher idocPublisher;

    // NOT USED - Commenting out to simplify
    // @Autowired
    // private org.dataingest.rfc.server.idoc.UnifiedIDOCReceiver unifiedIDOCReceiver;

    @Autowired
    private IDOCServerTIDHandler tidHandler;

    // NOT USED - Commenting out to simplify
    // @Autowired
    // private RFCFunctionCallListener rfcFunctionCallListener;

    // NOT USED - Commenting out to simplify
    // @Autowired(required = false)
    // private SAPEnvironmentInitializer sapEnvironmentInitializer;

    // RFC Server Configuration
    @Value("${jco.server.enabled:false}")
    private boolean enabled;

    @Value("${jco.server.gwhost:localhost}")
    private String gwhost;

    @Value("${jco.server.gwserv:3300}")
    private String gwserv;

    @Value("${jco.server.progid:KAFKA_RFC}")
    private String progid;

    @Value("${jco.server.connection_count:4}")
    private int connectionCount;

    @Value("${jco.server.worker_thread_count:#{null}}")
    private Integer workerThreadCount;

    @Value("${jco.server.worker_thread_min_count:#{null}}")
    private Integer workerThreadMinCount;

    @Value("${jco.server.saprouter:}")
    private String saprouter;

    @Value("${jco.server.trace:0}")
    private int trace;

    // NOT USED - Client config not needed for simple IDoc receive
    // @Value("${jco.client.ashost:localhost}")
    // private String ashost;
    // @Value("${jco.client.client:100}")
    // private String client;
    // @Value("${jco.client.lang:en}")
    // private String lang;
    // @Value("${jco.client.user:rfc_user}")
    // private String user;
    // @Value("${jco.client.passwd:}")
    // private String passwd;
    // @Value("${jco.client.sysnr:00}")
    // private String sysnr;
    // @Value("${jco.client.trace:0}")
    // private int clientTrace;

    private JCoIDocServer rfcServer;  // Changed from JCoServer to JCoIDocServer
    private boolean serverStarted = false;

    /**
     * Initializes the RFC Server on application startup.
     * Called automatically by Spring after the bean is created.
     */
    @PostConstruct
    public void init() {
        if (!enabled) {
            LOGGER.info("SAP RFC Server is disabled (jco.server.enabled=false)");
            LOGGER.info("IDOC data must be published directly via IDocKafkaPublisher");
            return;
        }

        LOGGER.info("================================================");
        LOGGER.info("SAP RFC Server Configuration:");
        LOGGER.info("  Gateway Host: {}", gwhost);
        LOGGER.info("  Gateway Service: {}", gwserv);
        LOGGER.info("  Program ID: {}", progid);
        LOGGER.info("  Connection Count: {}", connectionCount);
        LOGGER.info("  SAP Router: {}", saprouter.isEmpty() ? "Not configured" : saprouter);
        LOGGER.info("  Trace Level: {}", trace);
        LOGGER.info("================================================");

        try {
            startRFCServer();
        } catch (Exception e) {
            LOGGER.error("Failed to start SAP RFC Server: {}", e.getMessage(), e);
            LOGGER.warn("Application will continue without RFC Server. IDOC data must be published directly.");
        }
    }

    /**
     * Starts the SAP RFC Server and registers with gateway.
     *
     * Creates a JCoServer instance, registers the handler factory,
     * and starts listening on the SAP Gateway.
     *
     * Note: The SAPEnvironmentInitializer must have already registered the
     * ServerDataProvider with the SAP environment.
     */
    private void startRFCServer() throws Exception {
        LOGGER.info("Starting SAP RFC Server with Program ID: {}", progid);
        LOGGER.debug("  Gateway Host: {}", gwhost);
        LOGGER.debug("  Gateway Service: {}", gwserv);
        LOGGER.debug("  Connection Count: {}", connectionCount);

        try {
            // EXACTLY like SAP's IDocServerExample.java
            rfcServer = JCoIDoc.getServer(progid);
            rfcServer.setIDocHandlerFactory(new MyIDocHandlerFactory());
            rfcServer.setTIDHandler(tidHandler);
            rfcServer.addServerErrorListener((JCoServerErrorListener) (server, msg, info, error) -> {
                LOGGER.error("SAP JCo SERVER ERROR: {}", msg, error);
            });
            rfcServer.addServerExceptionListener((JCoServerExceptionListener) (server, msg, info, exception) -> {
                LOGGER.error("SAP JCo SERVER EXCEPTION: {}", msg, exception);
            });
            rfcServer.start();
            serverStarted = true;

            LOGGER.info("================================================");
            LOGGER.info("SAP RFC Server STARTED - Pattern: SAP IDocServerExample");
            LOGGER.info("  Program ID: {}", progid);
            LOGGER.info("  Gateway: {}:{}", gwhost, gwserv);
            LOGGER.info("================================================");

        } catch (Exception e) {
            LOGGER.error("Failed to start RFC Server: {}", e.getMessage(), e);
            serverStarted = false;
            throw new Exception("RFC Server initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the complete property set for RFC Server initialization.
     *
     * Note: These properties are now provided by SAPServerDataProvider
     * instead of being used directly.
     *
     * @return Properties object with all RFC server settings
     */
    private Properties buildServerProperties() {
        Properties props = new Properties();

        // Gateway Connection Settings
        props.setProperty("jco.server.gwhost", gwhost);
        props.setProperty("jco.server.gwserv", gwserv);
        props.setProperty("jco.server.progid", progid);
        props.setProperty("jco.server.connection_count", String.valueOf(connectionCount));

        // Optional Worker Thread Settings
        if (workerThreadCount != null) {
            props.setProperty("jco.server.worker_thread_count", String.valueOf(workerThreadCount));
        }
        if (workerThreadMinCount != null) {
            props.setProperty("jco.server.worker_thread_min_count", String.valueOf(workerThreadMinCount));
        }

        // SAP Router (if behind firewall)
        if (!saprouter.isEmpty()) {
            props.setProperty("jco.server.saprouter", saprouter);
        }

        // Trace Settings
        props.setProperty("jco.server.trace", String.valueOf(trace));

        // Client Authentication Settings
        props.setProperty("jco.client.ashost", ashost);
        props.setProperty("jco.client.client", client);
        props.setProperty("jco.client.lang", lang);
        props.setProperty("jco.client.user", user);
        props.setProperty("jco.client.passwd", passwd);
        props.setProperty("jco.client.sysnr", sysnr);
        props.setProperty("jco.client.trace", String.valueOf(clientTrace));

        return props;
    }

    /**
     * Returns the current status of the RFC Server.
     */
    public String getServerInfo() {
        String status = enabled ? (serverStarted ? "RUNNING" : "FAILED") : "DISABLED";
        return String.format("RFC Server Status: %s (Program ID: %s, Gateway: %s:%s)",
                status, progid, gwhost, gwserv);
    }

    /**
     * Stops the RFC Server gracefully on application shutdown.
     */
    @EventListener
    public void onApplicationShutdown(ContextClosedEvent event) {
        if (enabled && serverStarted) {
            LOGGER.info("Stopping SAP RFC Server (Program ID: {})", progid);
            try {
                stopRFCServer();
                serverStarted = false;
            } catch (Exception e) {
                LOGGER.error("Error stopping RFC Server: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Stops the RFC Server.
     */
    private void stopRFCServer() throws Exception {
        LOGGER.info("Stopping RFC Server");
        if (rfcServer != null) {
            try {
                rfcServer.stop();
                LOGGER.info("RFC Server stopped successfully");
            } catch (Exception e) {
                LOGGER.error("Error stopping RFC Server: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * Returns whether the RFC Server is currently enabled and running.
     */
    public boolean isRunning() {
        return enabled && serverStarted && rfcServer != null;
    }

    // ========================================================================
    // Inner classes - following SAP IDocServerExample pattern exactly
    // ========================================================================

    /**
     * IDoc Handler Factory - exactly like SAP's MyIDocHandlerFactory
     */
    class MyIDocHandlerFactory implements JCoIDocHandlerFactory {
        private JCoIDocHandler handler = new MyIDocReceiveHandler();

        @Override
        public JCoIDocHandler getIDocHandler(JCoIDocServerContext serverCtx) {
            System.err.println("!!!!! MyIDocHandlerFactory.getIDocHandler() CALLED !!!!!");
            LOGGER.error("!!!!! MyIDocHandlerFactory.getIDocHandler() CALLED !!!!!");
            LOGGER.error("!!!!! Returning handler: {}", handler.getClass().getName());
            return handler;
        }
    }

    /**
     * IDoc Receive Handler - EXACTLY like SAP's MyIDocReceiveHandler
     */
    class MyIDocReceiveHandler implements JCoIDocHandler {
        @Override
        public void handleRequest(JCoServerContext serverCtx, IDocDocumentList idocList) {
            System.err.println("!!!!! IDoc Handler Called!");
            LOGGER.error("!!!!! IDoc Handler Called!");
            LOGGER.error("!!!!! TID: {}", serverCtx.getTID());
            LOGGER.error("!!!!! IDoc Count: {}", idocList.getNumDocuments());

            // EXACTLY like SAP example - save to XML file
            java.io.FileOutputStream fos = null;
            java.io.OutputStreamWriter osw = null;
            try {
                com.sap.conn.idoc.IDocXMLProcessor xmlProcessor = JCoIDoc.getIDocFactory().getIDocXMLProcessor();
                String filename = serverCtx.getTID() + "_idoc.xml";
                fos = new java.io.FileOutputStream(filename);
                osw = new java.io.OutputStreamWriter(fos, "UTF8");
                xmlProcessor.render(idocList, osw, com.sap.conn.idoc.IDocXMLProcessor.RENDER_WITH_TABS_AND_CRLF);
                osw.flush();
                LOGGER.error("!!!!! IDoc saved to: {}", filename);
                System.err.println("!!!!! IDoc saved to: " + filename);
            } catch (Exception e) {
                LOGGER.error("Error saving IDoc: {}", e.getMessage(), e);
            } finally {
                try {
                    if (osw != null) osw.close();
                    if (fos != null) fos.close();
                } catch (Exception e) {
                    LOGGER.error("Error closing file: {}", e.getMessage());
                }
            }
        }
    }
}

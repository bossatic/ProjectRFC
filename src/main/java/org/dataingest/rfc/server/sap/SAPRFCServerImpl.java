package org.dataingest.rfc.server.sap;

import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerFactory;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.DefaultServerHandlerFactory;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.JCoFunctionTemplate;
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

    @Autowired
    private IDocKafkaPublisher idocPublisher;

    @Autowired
    private org.dataingest.rfc.server.idoc.UnifiedIDOCReceiver unifiedIDOCReceiver;

    @Autowired
    private IDOCServerTIDHandler tidHandler;

    @Autowired
    private RFCFunctionCallListener rfcFunctionCallListener;

    @Autowired(required = false)
    private SAPEnvironmentInitializer sapEnvironmentInitializer;

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

    // RFC Client Configuration (for server authentication to SAP if needed)
    @Value("${jco.client.ashost:localhost}")
    private String ashost;

    @Value("${jco.client.client:100}")
    private String client;

    @Value("${jco.client.lang:en}")
    private String lang;

    @Value("${jco.client.user:rfc_user}")
    private String user;

    @Value("${jco.client.passwd:}")
    private String passwd;

    @Value("${jco.client.sysnr:00}")
    private String sysnr;

    @Value("${jco.client.trace:0}")
    private int clientTrace;

    private JCoServer rfcServer;
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

        LOGGER.debug("RFC Server Configuration:");
        LOGGER.debug("  Gateway Host: {}", gwhost);
        LOGGER.debug("  Gateway Service: {}", gwserv);
        LOGGER.debug("  Program ID: {}", progid);
        LOGGER.debug("  Connection Count: {}", connectionCount);

        try {
            // Get RFC Server instance from factory
            // The ServerDataProvider (registered in SAPEnvironmentInitializer)
            // will supply the configuration for this server name
            rfcServer = JCoServerFactory.getServer(progid);
            LOGGER.info("RFC Server instance obtained for Program ID: {}", progid);

            // Register error listener to capture SAP JCo errors
            rfcServer.addServerErrorListener((JCoServerErrorListener) (server, msg, info, error) -> {
                System.err.println("!!!!! SERVER ERROR LISTENER CALLED !!!!!");
                LOGGER.error("=== SAP JCo SERVER ERROR DETECTED ===");
                LOGGER.error("Message: {}", msg);
                LOGGER.error("Error: {}", error);
                LOGGER.error("Stack Trace: ", error);
            });
            LOGGER.info("Server error listener registered");

            // Register exception listener to capture SAP JCo exceptions
            rfcServer.addServerExceptionListener((JCoServerExceptionListener) (server, msg, info, exception) -> {
                System.err.println("!!!!! SERVER EXCEPTION LISTENER CALLED !!!!!");
                LOGGER.error("=== SAP JCo SERVER EXCEPTION DETECTED ===");
                LOGGER.error("Message: {}", msg);
                LOGGER.error("Exception: {}", exception.getMessage());
                LOGGER.error("Stack Trace: ", exception);
            });
            LOGGER.info("Server exception listener registered");

            // Verify repository is loaded
            try {
                LOGGER.error("=== VERIFYING REPOSITORY AND FUNCTIONS ===");

                JCoRepository repo = rfcServer.getRepository();
                if (repo != null) {
                    LOGGER.error("✓ Repository obtained from server");
                    try {
                        JCoFunctionTemplate fnTemplate = repo.getFunctionTemplate("IDOC_INBOUND_ASYNCHRONOUS");
                        if (fnTemplate != null) {
                            LOGGER.error("✓ IDOC_INBOUND_ASYNCHRONOUS function found in repository!");
                            LOGGER.error("  Function Name: {}", fnTemplate.getName());
                            LOGGER.error("  Import parameters: {}", fnTemplate.getImportParameterList() != null ? fnTemplate.getImportParameterList().getFieldCount() : 0);
                        } else {
                            LOGGER.error("✗ IDOC_INBOUND_ASYNCHRONOUS function NOT found in repository!");
                        }
                    } catch (Exception e) {
                        LOGGER.error("✗ Error accessing function: {}", e.getMessage(), e);
                    }
                } else {
                    LOGGER.error("✗ Repository is NULL!");
                }
            } catch (Exception e) {
                LOGGER.error("✗ Error verifying repository: {}", e.getMessage(), e);
            }

            // CORRECT APPROACH: Use RFCFunctionCallListener to intercept function calls
            // This listener explicitly calls the UnifiedIDOCReceiver's handleRequest method
            if (rfcFunctionCallListener == null) {
                LOGGER.error("!!!!! ERROR: rfcFunctionCallListener is NULL - Spring failed to autowire it !!!!!");
                throw new RuntimeException("RFCFunctionCallListener not autowired");
            }

            try {
                // Create a FunctionHandlerFactory (correct API usage)
                DefaultServerHandlerFactory.FunctionHandlerFactory functionHandlerFactory =
                    new DefaultServerHandlerFactory.FunctionHandlerFactory();

                System.err.println("!!!!! FunctionHandlerFactory created successfully");

                // Register RFCFunctionCallListener for IDOC_INBOUND_ASYNCHRONOUS function
                // This listener will explicitly call UnifiedIDOCReceiver.handleRequest()
                functionHandlerFactory.registerHandler("IDOC_INBOUND_ASYNCHRONOUS", rfcFunctionCallListener);
                System.err.println("!!!!! RFCFunctionCallListener registered for IDOC_INBOUND_ASYNCHRONOUS");
                LOGGER.error("!!!!! RFCFunctionCallListener registered for IDOC_INBOUND_ASYNCHRONOUS");

                // CRITICAL: Also register as GENERIC handler to catch ANY function that doesn't match
                // This will help us discover what function name SAP is actually calling
                functionHandlerFactory.registerGenericHandler(rfcFunctionCallListener);
                System.err.println("!!!!! RFCFunctionCallListener registered as generic handler (fallback)");
                LOGGER.error("!!!!! RFCFunctionCallListener registered as generic handler (fallback)");

                // Set the factory on the server
                rfcServer.setCallHandlerFactory(functionHandlerFactory);
                System.err.println("!!!!! FunctionHandlerFactory set on RFC Server");
                LOGGER.error("!!!!! FunctionHandlerFactory set on RFC Server");

            } catch (Exception e) {
                System.err.println("!!!!! ERROR registering handler: " + e.getMessage());
                e.printStackTrace();
                LOGGER.error("!!!!! ERROR registering handler: {}", e.getMessage(), e);
                throw e;
            }

            LOGGER.error("!!!!! RFC FUNCTION CALL LISTENER REGISTERED !!!!!!!");
            System.err.println("!!!!! RFC FUNCTION CALL LISTENER REGISTERED !!!!!!!");

            // Register the TID handler AFTER setting CallHandlerFactory (matching Talend's order)
            rfcServer.setTIDHandler(tidHandler);
            LOGGER.info("TID handler registered with RFC Server");

            // Start the server listening on SAP Gateway
            rfcServer.start();
            serverStarted = true;
            LOGGER.info("RFC Server started and listening on gateway: {}:{}", gwhost, gwserv);

            LOGGER.info("================================================");
            LOGGER.info("SAP RFC Server Configuration Ready:");
            LOGGER.info("  Program ID: {}", progid);
            LOGGER.info("  Gateway: {}:{}", gwhost, gwserv);
            LOGGER.info("  Unified Receiver: {}", unifiedIDOCReceiver.getClass().getSimpleName());
            LOGGER.info("  Status: Listening for RFC calls from SAP");
            LOGGER.info("================================================");
            LOGGER.info("IDOCs will be published to Kafka topics");

        } catch (Exception e) {
            LOGGER.error("Failed to initialize RFC Server: {}", e.getMessage(), e);
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
}

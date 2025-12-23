package org.dataingest.rfc.server.config;

import com.sap.conn.jco.ext.ServerDataProvider;
import com.sap.conn.jco.ext.ServerDataEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Properties;

/**
 * SAP JCo ServerDataProvider for RFC Server configuration.
 *
 * Supplies server configuration properties to the SAP JCo environment.
 * This is required by JCoServerFactory to configure RFC server instances.
 */
@Component
public class SAPServerDataProvider implements ServerDataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAPServerDataProvider.class);

    private ServerDataEventListener eventListener;

    // RFC Server Configuration
    @Value("${jco.server.gwhost:localhost}")
    private String gwhost;

    @Value("${jco.server.gwserv:sapgw00}")
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

    @Value("${jco.server.repository_file:}")
    private String repositoryFile;

    // RFC Client Configuration (for server authentication to SAP)
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

    /**
     * Returns the server configuration properties for the specified server name.
     *
     * Provides server configuration with gateway connection settings, connection pooling,
     * and client authentication to SAP system.
     *
     * @param serverName Name of the RFC server (e.g., "TALEND")
     * @return Properties object with server configuration
     */
    @Override
    public Properties getServerProperties(String serverName) {
        LOGGER.info("ServerDataProvider: Providing configuration for server: {}", serverName);

        Properties props = new Properties();

        // Gateway Connection Settings - REQUIRED
        props.setProperty("jco.server.gwhost", gwhost);
        props.setProperty("jco.server.gwserv", gwserv);
        props.setProperty("jco.server.progid", serverName);  // Use the requested server name
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

        // Repository Configuration - REQUIRED by SAP JCo
        // Use file-based repository (minimal/empty) to avoid authentication issues
        if (repositoryFile != null && !repositoryFile.isEmpty()) {
            props.setProperty("jco.server.repository_file", repositoryFile);
        }

        LOGGER.debug("Server properties for {}: gwhost={}, gwserv={}, connection_count={}, repository_file={}",
                     serverName, gwhost, gwserv, connectionCount, repositoryFile);
        return props;
    }

    /**
     * Sets the event listener for server data change events.
     *
     * @param eventListener Listener to handle server data events
     */
    @Override
    public void setServerDataEventListener(ServerDataEventListener eventListener) {
        this.eventListener = eventListener;
        LOGGER.debug("ServerDataEventListener registered");
    }

    /**
     * Indicates whether this provider supports server data events.
     *
     * @return true if events are supported
     */
    @Override
    public boolean supportsEvents() {
        return false;  // We don't support dynamic event notifications for now
    }
}

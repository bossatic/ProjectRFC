package org.dataingest.rfc.server.config;

import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Properties;

/**
 * SAP JCo DestinationDataProvider for RFC Client configuration.
 *
 * Supplies client destination configuration to the SAP JCo environment.
 * This is required by RFC servers to retrieve function metadata from SAP.
 */
@Component
public class SAPClientDataProvider implements DestinationDataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SAPClientDataProvider.class);

    private DestinationDataEventListener eventListener;

    // RFC Client Configuration
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
    private int trace;

    /**
     * Returns the client destination properties for the specified destination name.
     *
     * @param destinationName Name of the destination (e.g., "TALEND_REPOSITORY")
     * @return Properties object with client configuration
     */
    @Override
    public Properties getDestinationProperties(String destinationName) {
        LOGGER.info("DestinationDataProvider: Providing destination properties for: {}", destinationName);

        Properties props = new Properties();

        // Client Authentication Settings - REQUIRED for repository access
        props.setProperty("jco.client.ashost", ashost);
        props.setProperty("jco.client.client", client);
        props.setProperty("jco.client.lang", lang);
        props.setProperty("jco.client.user", user);
        props.setProperty("jco.client.passwd", passwd);
        props.setProperty("jco.client.sysnr", sysnr);
        props.setProperty("jco.client.trace", String.valueOf(trace));

        LOGGER.debug("Destination properties for {}: ashost={}, client={}, sysnr={}",
                     destinationName, ashost, client, sysnr);
        return props;
    }

    /**
     * Sets the event listener for destination data change events.
     *
     * @param eventListener Listener to handle destination data events
     */
    @Override
    public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
        this.eventListener = eventListener;
        LOGGER.debug("DestinationDataEventListener registered");
    }

    /**
     * Indicates whether this provider supports destination data events.
     *
     * @return true if events are supported
     */
    @Override
    public boolean supportsEvents() {
        return false;  // We don't support dynamic event notifications
    }
}

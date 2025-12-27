package org.dataingest.rfc.server.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.ServerDataEventListener;
import com.sap.conn.jco.ext.ServerDataProvider;

/**
 * JCo Destination and Server Data Provider
 * Reads configuration from .jcoDestination files in the current working directory
 */
public class JCoDestinationDataProvider implements DestinationDataProvider, ServerDataProvider {

    private static final String FILE_EXTENSION = ".jcoDestination";

    @Override
    public Properties getDestinationProperties(String destinationName) {
        return readPropertiesFile(destinationName);
    }

    @Override
    public Properties getServerProperties(String serverName) {
        return readPropertiesFile(serverName);
    }

    @Override
    public void setDestinationDataEventListener(DestinationDataEventListener listener) {
        // Optional: implement if you want to listen to changes
    }

    @Override
    public void setServerDataEventListener(ServerDataEventListener listener) {
        // Optional: implement if you want to listen to changes
    }

    @Override
    public boolean supportsEvents() {
        return false;
    }

    /**
     * Reads properties from .jcoDestination file
     */
    private Properties readPropertiesFile(String fileName) {
        Properties properties = new Properties();
        String filePath = fileName + FILE_EXTENSION;

        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
            return properties;
        } catch (IOException e) {
            System.err.println("Error reading JCo configuration file: " + filePath);
            return null;
        }
    }
}

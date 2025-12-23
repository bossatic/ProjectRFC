package org.dataingest.rfc.server.util;

import org.talend.sap.bw.ISAPBWDataRequest;

/**
 * Utility for generating Kafka topic names for BW data source requests.
 *
 * Topic names follow the pattern: SAP.DATASOURCES.{DATASOURCE_NAME}
 * Where special characters are replaced with underscores.
 */
public class BWDataTopicNameUtil {

    private static final String TOPIC_PREFIX = "SAP.DATASOURCES.";

    /**
     * Generates a Kafka topic name for the given BW data request.
     *
     * @param dataRequest the BW data request
     * @return the topic name
     */
    public static String getTopicName(ISAPBWDataRequest dataRequest) {
        String dataSourceName = dataRequest.getDataSourceName() != null
            ? dataRequest.getDataSourceName()
            : "UNKNOWN";

        String validName = toValidStructName(dataSourceName);
        return TOPIC_PREFIX + validName;
    }

    /**
     * Converts a string to a valid Kafka topic name by replacing invalid characters with underscores.
     *
     * Kafka topic names can only contain alphanumeric characters, dots, hyphens, and underscores.
     *
     * @param name the string to convert
     * @return the valid topic name component
     */
    private static String toValidStructName(String name) {
        if (name == null || name.isEmpty()) {
            return "UNKNOWN";
        }

        // Replace all invalid characters with underscores
        // Valid characters: a-z, A-Z, 0-9, _, -, .
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}

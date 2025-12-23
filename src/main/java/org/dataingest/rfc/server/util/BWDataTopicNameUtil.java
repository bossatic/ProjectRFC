package org.dataingest.rfc.server.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility for generating Kafka topic names for BW data source requests.
 *
 * Topic names follow the pattern configured in application.properties (kafka.bw.topic.prefix)
 * plus {DATASOURCE_NAME} where special characters are replaced with underscores.
 *
 * Default pattern: SAP.DATASOURCES.{DATASOURCE_NAME}
 * Examples:
 * - SAP.DATASOURCES.0MATERIAL_ATTR
 * - SAP.DATASOURCES.0VENDOR_ATTR
 * - SAP.DATASOURCES.0CUSTOMER_ATTR
 * - SAP.DATASOURCES.0COMPANY_ATTR
 */
@Component
public class BWDataTopicNameUtil {

    @Value("${kafka.bw.topic.prefix:SAP.DATASOURCES}")
    private String topicPrefix;

    /**
     * Generates a Kafka topic name for the given BW data source name.
     *
     * Topic format: {prefix}.{DATA_SOURCE_NAME}
     *
     * @param dataSourceName the BW data source name (e.g., 0MATERIAL_ATTR)
     * @return the topic name (e.g., SAP.DATASOURCES.0MATERIAL_ATTR)
     */
    public String getTopicName(String dataSourceName) {
        String name = dataSourceName != null ? dataSourceName : "UNKNOWN";
        String validName = sanitize(name);
        return topicPrefix + "." + validName;
    }

    /**
     * Converts a string to a valid Kafka topic name by replacing invalid characters.
     *
     * Kafka topic names can only contain:
     * - Alphanumeric characters (a-z, A-Z, 0-9)
     * - Dots (.)
     * - Hyphens (-)
     * - Underscores (_)
     *
     * All other characters are replaced with underscores.
     *
     * @param name the string to sanitize
     * @return the sanitized topic name component
     */
    private String sanitize(String name) {
        if (name == null || name.isEmpty()) {
            return "UNKNOWN";
        }

        // Replace all invalid characters with underscores
        // Valid characters: a-z, A-Z, 0-9, _, -, .
        String sanitized = name.replaceAll("[^a-zA-Z0-9_.-]", "_");

        // Remove leading/trailing underscores
        sanitized = sanitized.replaceAll("^_+|_+$", "");

        return sanitized.isEmpty() ? "UNKNOWN" : sanitized;
    }
}

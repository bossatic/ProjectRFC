package org.dataingest.rfc.server.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.dataingest.rfc.server.model.SAPIDOCDocument;

/**
 * Utility for generating Kafka topic names for IDOC messages.
 *
 * Topic names follow the pattern configured in application.properties (kafka.idoc.topic.prefix)
 * plus {TYPE}_{VERSION} where special characters are replaced with underscores.
 *
 * Default pattern: SAP.IDOCS.{TYPE}_{VERSION}
 * Examples:
 * - SAP.IDOCS.ORDERS_05
 * - SAP.IDOCS.INVOIC_01
 * - SAP.IDOCS.DESADV_01
 * - SAP.IDOCS.MATMAS_05
 */
@Component
public class IDocTopicNameUtil {

    @Value("${kafka.idoc.topic.prefix:SAP.IDOCS}")
    private String topicPrefix;

    /**
     * Generates a Kafka topic name for the given SAP IDOC document.
     *
     * Topic format: {prefix}.{MESSAGE_TYPE}_{MESSAGE_VERSION}
     *
     * @param document the SAP IDOC document
     * @return the topic name (e.g., SAP.IDOCS.ORDERS_05)
     */
    public String getTopicName(SAPIDOCDocument document) {
        if (document == null) {
            return topicPrefix + ".UNKNOWN";
        }

        String type = document.getMessageType() != null
            ? document.getMessageType()
            : "UNKNOWN";

        String version = document.getMessageTypeVersion() != null
            ? document.getMessageTypeVersion()
            : "000";

        String structName = sanitize(type) + "_" + sanitize(version);
        return topicPrefix + "." + structName;
    }

    /**
     * Generates a Kafka topic name from message type and version.
     *
     * @param messageType the IDOC message type (e.g., ORDERS)
     * @param messageVersion the IDOC message version (e.g., 05)
     * @return the topic name
     */
    public String getTopicName(String messageType, String messageVersion) {
        String type = messageType != null ? messageType : "UNKNOWN";
        String version = messageVersion != null ? messageVersion : "000";

        String structName = sanitize(type) + "_" + sanitize(version);
        return topicPrefix + "." + structName;
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
        // Invalid characters are replaced with _
        String sanitized = name.replaceAll("[^a-zA-Z0-9_.-]", "_");

        // Remove leading/trailing underscores
        sanitized = sanitized.replaceAll("^_+|_+$", "");

        return sanitized.isEmpty() ? "UNKNOWN" : sanitized;
    }
}

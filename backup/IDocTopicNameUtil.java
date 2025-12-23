package org.dataingest.rfc.server.util;

import org.talend.sap.idoc.ISAPIDoc;

/**
 * Utility for generating Kafka topic names for IDOC messages.
 *
 * Topic names follow the pattern: SAP.IDOCS.{TYPE}_{EXTENSION}
 * Where special characters are replaced with underscores.
 */
public class IDocTopicNameUtil {

    private static final String TOPIC_PREFIX = "SAP.IDOCS.";

    /**
     * Generates a Kafka topic name for the given IDOC.
     *
     * @param idoc the IDOC
     * @return the topic name
     */
    public static String getTopicName(ISAPIDoc idoc) {
        String type = idoc.getType() != null ? idoc.getType() : "UNKNOWN";
        String extension = idoc.getExtension() != null ? idoc.getExtension() : "000";

        String structName = toValidStructName(type) + "_" + toValidStructName(extension);
        return TOPIC_PREFIX + structName;
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

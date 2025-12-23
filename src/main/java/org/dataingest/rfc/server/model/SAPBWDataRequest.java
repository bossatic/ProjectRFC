package org.dataingest.rfc.server.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a SAP BW (Business Warehouse) data request.
 *
 * Wraps official SAP BW data source requests for Kafka publishing.
 */
public class SAPBWDataRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;              // Request ID
    private String dataSourceName;         // Data source name (e.g., 0MATERIAL_ATTR)
    private String logicalSystem;          // Logical system
    private Map<String, String> parameters; // Request parameters
    private String requestData;            // Raw request data
    private long timestamp;                // When received

    public SAPBWDataRequest() {
        this.parameters = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getLogicalSystem() {
        return logicalSystem;
    }

    public void setLogicalSystem(String logicalSystem) {
        this.logicalSystem = logicalSystem;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(String key, String value) {
        this.parameters.put(key, value);
    }

    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get topic name for this BW data request
     * Format: SAP.DATASOURCES.{DATA_SOURCE_NAME}
     * Example: SAP.DATASOURCES.0MATERIAL_ATTR
     */
    public String getTopicName() {
        if (dataSourceName == null) {
            return "SAP.DATASOURCES.UNKNOWN";
        }
        return "SAP.DATASOURCES." + dataSourceName;
    }

    @Override
    public String toString() {
        return "SAPBWDataRequest{" +
                "requestId='" + requestId + '\'' +
                ", dataSourceName='" + dataSourceName + '\'' +
                ", logicalSystem='" + logicalSystem + '\'' +
                ", parameters=" + parameters.size() +
                ", timestamp=" + timestamp +
                '}';
    }
}

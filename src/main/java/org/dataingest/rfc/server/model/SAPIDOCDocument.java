package org.dataingest.rfc.server.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a SAP IDOC document received from SAP system via RFC call.
 *
 * Wraps the official SAP IDOC library to provide consistent interface
 * for Kafka publishing and transaction management.
 */
public class SAPIDOCDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    private String documentNumber;      // DOCNUM
    private String messageType;          // IDOCTYP (e.g., ORDERS, INVOIC)
    private String messageTypeVersion;   // IDOCVER (e.g., 01, 05)
    private String senderParty;         // SNDPRT
    private String senderPort;          // SNDPOR
    private String senderSystem;        // SNDSYS
    private String receiverParty;       // RCVPRT
    private String receiverPort;        // RCVPOR
    private String receiverSystem;      // RCVSYS
    private List<String> segmentData;   // Raw segment data
    private String transactionID;       // tRFC/qRFC transaction ID
    private long timestamp;             // When received

    public SAPIDOCDocument() {
        this.segmentData = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageTypeVersion() {
        return messageTypeVersion;
    }

    public void setMessageTypeVersion(String messageTypeVersion) {
        this.messageTypeVersion = messageTypeVersion;
    }

    public String getSenderParty() {
        return senderParty;
    }

    public void setSenderParty(String senderParty) {
        this.senderParty = senderParty;
    }

    public String getSenderPort() {
        return senderPort;
    }

    public void setSenderPort(String senderPort) {
        this.senderPort = senderPort;
    }

    public String getSenderSystem() {
        return senderSystem;
    }

    public void setSenderSystem(String senderSystem) {
        this.senderSystem = senderSystem;
    }

    public String getReceiverParty() {
        return receiverParty;
    }

    public void setReceiverParty(String receiverParty) {
        this.receiverParty = receiverParty;
    }

    public String getReceiverPort() {
        return receiverPort;
    }

    public void setReceiverPort(String receiverPort) {
        this.receiverPort = receiverPort;
    }

    public String getReceiverSystem() {
        return receiverSystem;
    }

    public void setReceiverSystem(String receiverSystem) {
        this.receiverSystem = receiverSystem;
    }

    public List<String> getSegmentData() {
        return segmentData;
    }

    public void setSegmentData(List<String> segmentData) {
        this.segmentData = segmentData;
    }

    public void addSegment(String segment) {
        this.segmentData.add(segment);
    }

    public String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get topic name based on IDOC type and version
     * Format: SAP.IDOCS.{TYPE}_{VERSION}
     * Example: SAP.IDOCS.ORDERS_05
     */
    public String getTopicName() {
        if (messageType == null || messageTypeVersion == null) {
            return "SAP.IDOCS.UNKNOWN";
        }
        return "SAP.IDOCS." + messageType + "_" + messageTypeVersion;
    }

    @Override
    public String toString() {
        return "SAPIDOCDocument{" +
                "documentNumber='" + documentNumber + '\'' +
                ", messageType='" + messageType + '\'' +
                ", messageTypeVersion='" + messageTypeVersion + '\'' +
                ", senderSystem='" + senderSystem + '\'' +
                ", receiverSystem='" + receiverSystem + '\'' +
                ", segments=" + segmentData.size() +
                ", timestamp=" + timestamp +
                '}';
    }
}

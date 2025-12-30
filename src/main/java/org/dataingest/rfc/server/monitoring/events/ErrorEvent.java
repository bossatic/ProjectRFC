package org.dataingest.rfc.server.monitoring.events;

/**
 * Event fired when an error occurs
 */
public class ErrorEvent extends MonitoringEvent {
    private final ErrorStage stage;
    private final String idocType;
    private final String docNum;
    private final String errorMessage;
    private final String stackTrace;
    private final boolean recoverable;

    public ErrorEvent(ErrorStage stage, String idocType, String docNum,
                     String errorMessage, String stackTrace, boolean recoverable) {
        super(EventType.ERROR);
        this.stage = stage;
        this.idocType = idocType;
        this.docNum = docNum;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.recoverable = recoverable;
    }

    public ErrorStage getStage() {
        return stage;
    }

    public String getIdocType() {
        return idocType;
    }

    @Override
    public String getDocNum() {
        return docNum;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    @Override
    public String toJson() {
        String sanitizedMessage = errorMessage != null ?
            errorMessage.replace("\"", "\\\"").replace("\n", "\\n") : "";
        String sanitizedStack = stackTrace != null ?
            stackTrace.replace("\"", "\\\"").replace("\n", "\\n") : "";

        return String.format(
            "{\"event_id\":\"%s\",\"timestamp\":\"%s\",\"event_type\":\"ERROR\"," +
            "\"payload\":{\"stage\":\"%s\",\"idoc_type\":\"%s\",\"doc_num\":\"%s\"," +
            "\"error_message\":\"%s\",\"stack_trace\":\"%s\",\"recoverable\":%b}}",
            getEventId(), getTimestamp(), stage,
            idocType != null ? idocType : "",
            docNum != null ? docNum : "",
            sanitizedMessage, sanitizedStack, recoverable
        );
    }
}

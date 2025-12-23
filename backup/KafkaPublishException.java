package org.dataingest.rfc.server.exception;

/**
 * Exception thrown when publishing data to Kafka fails.
 *
 * This exception is used to signal that a Kafka publish operation has failed,
 * which should trigger a rollback of the corresponding SAP transaction.
 */
public class KafkaPublishException extends Exception {

    /**
     * Constructs a KafkaPublishException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public KafkaPublishException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a KafkaPublishException with the specified detail message.
     *
     * @param message the detail message
     */
    public KafkaPublishException(String message) {
        super(message);
    }

    /**
     * Constructs a KafkaPublishException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public KafkaPublishException(Throwable cause) {
        super(cause);
    }
}

package org.dataingest.rfc.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Properties;

/**
 * Spring Boot Configuration for RFC Server.
 *
 * Provides core beans needed for IDOC and BW data processing with direct Kafka publishing.
 */
@Configuration
public class ApplicationConfiguration {

    /**
     * Creates and configures the Jackson ObjectMapper for JSON serialization.
     *
     * Includes JavaTimeModule for proper serialization of Java 8 date/time types.
     *
     * @return Configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java 8 date/time types support
        mapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps (use ISO-8601 format instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }

    /**
     * Creates and configures the Kafka Producer for publishing messages.
     *
     * Uses synchronous publishing with all-acks configuration to ensure delivery
     * before SAP transaction commits. All configuration comes from application.properties.
     *
     * @param bootstrapServers Kafka bootstrap servers
     * @param acks Acknowledgment mode
     * @param retries Number of retries
     * @param maxInFlightRequests Max in-flight requests per connection
     * @param compressionType Compression type
     * @param enableIdempotence Enable idempotent producer
     * @param requestTimeoutMs Request timeout in milliseconds
     * @param deliveryTimeoutMs Delivery timeout in milliseconds
     * @return Configured KafkaProducer instance
     */
    @Bean
    public Producer<String, String> kafkaProducer(
            @Value("${kafka.bootstrap.servers:localhost:9092}") String bootstrapServers,
            @Value("${kafka.acks:all}") String acks,
            @Value("${kafka.retries:3}") int retries,
            @Value("${kafka.max.in.flight.requests.per.connection:1}") int maxInFlightRequests,
            @Value("${kafka.compression.type:gzip}") String compressionType,
            @Value("${kafka.enable.idempotence:true}") boolean enableIdempotence,
            @Value("${kafka.request.timeout.ms:30000}") int requestTimeoutMs,
            @Value("${kafka.delivery.timeout.ms:120000}") int deliveryTimeoutMs) {

        Properties props = new Properties();

        // Bootstrap servers for Kafka cluster
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serializers for key and value
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Reliability settings for RFC (require all in-sync replicas to acknowledge)
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);

        // Ensure ordering by limiting in-flight requests
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequests);

        // Compression settings
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);

        // Idempotent producer for exactly-once semantics
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);

        // Request and delivery timeouts
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs);

        return new KafkaProducer<>(props);
    }
}

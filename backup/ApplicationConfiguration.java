package org.dataingest.rfc.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}

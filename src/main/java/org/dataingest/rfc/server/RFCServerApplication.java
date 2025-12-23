package org.dataingest.rfc.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * RFC Server - Main Spring Boot Application
 *
 * Kafka-based SAP RFC Server for IDOC and BW Data Publishing
 *
 * This application serves as the main entry point for the RFC Server,
 * which receives IDOCs and BW data requests from SAP systems via RFC
 * and publishes them directly to Kafka topics.
 *
 * Architecture:
 * - Receives IDOC/BW data from SAP via RFC calls
 * - Publishes messages to Kafka synchronously
 * - Commits SAP transactions on successful Kafka publish
 * - Rolls back SAP transactions on publish failures
 *
 * @author RFC Server Team
 * @version 1.0.0
 * @since 2024-12-18
 */
@SpringBootApplication
@ComponentScan(basePackages = "org.dataingest.rfc.server")
public class RFCServerApplication {

    /**
     * Main entry point for Spring Boot application
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(RFCServerApplication.class, args);
    }
}

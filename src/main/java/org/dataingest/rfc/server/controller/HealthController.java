package org.dataingest.rfc.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.dataingest.rfc.server.sap.SAPRFCServerImpl;
import java.util.HashMap;
import java.util.Map;

/**
 * Health and Status Endpoints for RFC Server
 */
@RestController
@RequestMapping("/")
public class HealthController {

    @Autowired(required = false)
    private SAPRFCServerImpl rfcServer;

    @GetMapping("health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "RFC Server");
        response.put("version", "1.0.0");
        return response;
    }

    @GetMapping("status")
    public Map<String, Object> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "RUNNING");

        // RFC Server Status
        if (rfcServer != null) {
            response.put("rfc.server.info", rfcServer.getServerInfo());
            response.put("rfc.server.running", rfcServer.isRunning());
        }

        // Kafka Status
        response.put("kafka.bootstrap.servers", "localhost:9092");
        response.put("kafka.status", "configured");

        // Publishing Status
        response.put("idoc.publishing", "enabled");
        response.put("bw.publishing", "enabled");

        return response;
    }
}

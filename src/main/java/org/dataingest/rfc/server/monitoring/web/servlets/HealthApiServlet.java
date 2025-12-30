package org.dataingest.rfc.server.monitoring.web.servlets;

import org.dataingest.rfc.server.config.IDocCaptureConfig;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Health check API servlet
 */
public class HealthApiServlet extends HttpServlet {
    private final IDocCaptureConfig config;
    private final Instant startTime = Instant.now();

    public HealthApiServlet(IDocCaptureConfig config) {
        this.config = config;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        long uptimeSeconds = Duration.between(startTime, Instant.now()).getSeconds();

        String json = String.format(
            "{\"status\":\"UP\",\"uptime_seconds\":%d,\"version\":\"1.0.0\"}",
            uptimeSeconds
        );

        resp.getWriter().write(json);
    }
}

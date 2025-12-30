package org.dataingest.rfc.server.monitoring.web;

import org.dataingest.rfc.server.config.IDocCaptureConfig;
import org.dataingest.rfc.server.monitoring.MetricsStore;
import org.dataingest.rfc.server.monitoring.db.H2QueryService;
import org.dataingest.rfc.server.monitoring.web.servlets.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Embedded Jetty web server for monitoring dashboard
 */
public class EmbeddedWebServer {
    private Server server;
    private final IDocCaptureConfig config;
    private final MetricsStore metricsStore;
    private final H2QueryService queryService;
    private SseServlet sseServlet;

    public EmbeddedWebServer(IDocCaptureConfig config, MetricsStore metricsStore, H2QueryService queryService) {
        this.config = config;
        this.metricsStore = metricsStore;
        this.queryService = queryService;
    }

    public void start() throws Exception {
        int port = config.getMonitoringDashboardPort();
        String host = config.getMonitoringDashboardHost();

        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // API Servlets
        context.addServlet(new ServletHolder(new HealthApiServlet(config)), "/api/health");
        context.addServlet(new ServletHolder(new MetricsApiServlet(metricsStore)), "/api/metrics");
        context.addServlet(new ServletHolder(new MetricsSummaryServlet(metricsStore)), "/api/metrics/summary");
        context.addServlet(new ServletHolder(new HistoryApiServlet(queryService)), "/api/history/*");
        context.addServlet(new ServletHolder(new StatsApiServlet(queryService)), "/api/stats/*");

        // SSE Servlet for real-time updates
        sseServlet = new SseServlet(metricsStore, config);
        context.addServlet(new ServletHolder(sseServlet), "/api/sse");

        // Static resource handler for dashboard files
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setResourceBase("./dashboard");

        HandlerList handlers = new HandlerList();
        handlers.addHandler(resourceHandler);
        handlers.addHandler(context);

        server.setHandler(handlers);
        server.start();

        // Start SSE broadcasting
        sseServlet.start();

        config.log("[Monitoring] Dashboard started at http://" + host + ":" + port);
    }

    public void stop() throws Exception {
        if (sseServlet != null) {
            sseServlet.stop();
        }
        if (server != null) {
            server.stop();
            config.log("[Monitoring] Dashboard stopped");
        }
    }

    public SseServlet getSseServlet() {
        return sseServlet;
    }
}

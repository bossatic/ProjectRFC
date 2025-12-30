package org.dataingest.rfc.server.monitoring.web.servlets;

import org.dataingest.rfc.server.monitoring.MetricsSnapshot;
import org.dataingest.rfc.server.monitoring.MetricsStore;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Metrics summary API servlet - returns condensed metrics
 */
public class MetricsSummaryServlet extends HttpServlet {
    private final MetricsStore metricsStore;

    public MetricsSummaryServlet(MetricsStore metricsStore) {
        this.metricsStore = metricsStore;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        MetricsSnapshot snapshot = metricsStore.getSnapshot();

        String json = String.format(
            "{\"total\":%d,\"success\":%d,\"failed\":%d,\"successRate\":%.2f,\"currentRate\":%.2f}",
            snapshot.getTotalIdocsReceived(),
            snapshot.getTotalSuccess(),
            snapshot.getTotalErrors(),
            snapshot.getSuccessRate(),
            snapshot.getReceptionRatePer5Min()
        );

        resp.getWriter().write(json);
    }
}

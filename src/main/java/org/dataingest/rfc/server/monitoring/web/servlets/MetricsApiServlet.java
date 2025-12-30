package org.dataingest.rfc.server.monitoring.web.servlets;

import org.dataingest.rfc.server.monitoring.MetricsSnapshot;
import org.dataingest.rfc.server.monitoring.MetricsStore;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Metrics API servlet - returns full metrics snapshot
 */
public class MetricsApiServlet extends HttpServlet {
    private final MetricsStore metricsStore;

    public MetricsApiServlet(MetricsStore metricsStore) {
        this.metricsStore = metricsStore;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        MetricsSnapshot snapshot = metricsStore.getSnapshot();
        resp.getWriter().write(snapshot.toJson());
    }
}

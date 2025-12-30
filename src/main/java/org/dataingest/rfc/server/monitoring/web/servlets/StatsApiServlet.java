package org.dataingest.rfc.server.monitoring.web.servlets;

import org.dataingest.rfc.server.monitoring.db.H2QueryService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Statistics API servlet - handles aggregate statistics
 */
public class StatsApiServlet extends HttpServlet {
    private final H2QueryService queryService;

    public StatsApiServlet(H2QueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            resp.setStatus(404);
            return;
        }

        // Placeholder - can be extended with actual stats queries
        resp.getWriter().write("{\"message\":\"Stats endpoint - to be implemented\"}");
    }
}

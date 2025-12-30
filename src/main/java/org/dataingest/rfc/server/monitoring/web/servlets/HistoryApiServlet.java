package org.dataingest.rfc.server.monitoring.web.servlets;

import org.dataingest.rfc.server.monitoring.db.H2QueryService;
import org.dataingest.rfc.server.monitoring.db.H2QueryService.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * History API servlet - handles historical data queries
 */
public class HistoryApiServlet extends HttpServlet {
    private final H2QueryService queryService;

    public HistoryApiServlet(H2QueryService queryService) {
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

        try {
            if (pathInfo.equals("/events")) {
                handleEventsQuery(req, resp);
            } else if (pathInfo.equals("/recent")) {
                handleRecentEventsQuery(req, resp);
            } else if (pathInfo.equals("/filtered")) {
                handleFilteredQuery(req, resp);
            } else if (pathInfo.equals("/idoc-types")) {
                handleIdocTypesQuery(req, resp);
            } else if (pathInfo.equals("/errors")) {
                handleErrorsQuery(req, resp);
            } else if (pathInfo.equals("/daily")) {
                handleDailyQuery(req, resp);
            } else {
                resp.setStatus(404);
            }
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleEventsQuery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String startStr = req.getParameter("start");
        String endStr = req.getParameter("end");
        String type = req.getParameter("type");
        int limit = Integer.parseInt(req.getParameter("limit") != null ? req.getParameter("limit") : "100");

        Instant start = Instant.parse(startStr);
        Instant end = Instant.parse(endStr);

        List<IdocEventDto> events = queryService.queryByDateRange(start, end, type, limit);

        StringBuilder json = new StringBuilder("{\"total\":" + events.size() + ",\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            IdocEventDto event = events.get(i);
            if (i > 0) json.append(",");
            json.append(String.format(
                "{\"eventId\":\"%s\",\"timestamp\":\"%s\",\"eventType\":\"%s\"," +
                "\"idocType\":\"%s\",\"docNum\":\"%s\",\"status\":\"%s\",\"processingTimeMs\":%d}",
                event.eventId, event.timestamp, event.eventType,
                event.idocType, event.docNum, event.status, event.processingTimeMs
            ));
        }
        json.append("]}");

        resp.getWriter().write(json.toString());
    }

    private void handleErrorsQuery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sinceStr = req.getParameter("since");
        int limit = Integer.parseInt(req.getParameter("limit") != null ? req.getParameter("limit") : "100");

        Instant since = Instant.parse(sinceStr);
        List<ErrorEventDto> errors = queryService.getErrors(since, limit);

        StringBuilder json = new StringBuilder("{\"errors\":[");
        for (int i = 0; i < errors.size(); i++) {
            ErrorEventDto error = errors.get(i);
            if (i > 0) json.append(",");
            json.append(String.format(
                "{\"timestamp\":\"%s\",\"idocType\":\"%s\",\"docNum\":\"%s\"," +
                "\"stage\":\"%s\",\"message\":\"%s\",\"recoverable\":%b}",
                error.timestamp, error.idocType, error.docNum,
                error.errorStage, error.errorMessage.replace("\"", "\\\""), error.recoverable
            ));
        }
        json.append("]}");

        resp.getWriter().write(json.toString());
    }

    private void handleRecentEventsQuery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int limit = Integer.parseInt(req.getParameter("limit") != null ? req.getParameter("limit") : "10");

        // Get recent received events
        Instant since = Instant.now().minusSeconds(3600); // Last hour
        List<IdocEventDto> events = queryService.queryByDateRange(
            since,
            Instant.now(),
            "IDOC_RECEIVED",
            limit
        );

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            IdocEventDto event = events.get(i);
            if (i > 0) json.append(",");
            json.append(String.format(
                "{\"time\":\"%s\",\"type\":\"%s\",\"docNum\":\"%s\",\"status\":\"success\"}",
                event.timestamp,
                event.idocType != null ? event.idocType : "",
                event.docNum != null ? event.docNum : ""
            ));
        }
        json.append("]");

        resp.getWriter().write(json.toString());
    }

    private void handleFilteredQuery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Parse filter parameters
        String timeRange = req.getParameter("timeRange");
        String idocType = req.getParameter("idocType");
        String eventType = req.getParameter("eventType");
        String status = req.getParameter("status");
        int limit = Integer.parseInt(req.getParameter("limit") != null ? req.getParameter("limit") : "50");

        // Calculate time range
        Instant endTime = Instant.now();
        Instant startTime;

        if (timeRange != null) {
            switch (timeRange) {
                case "1h":
                    startTime = endTime.minusSeconds(3600);
                    break;
                case "24h":
                    startTime = endTime.minusSeconds(86400);
                    break;
                case "7d":
                    startTime = endTime.minusSeconds(7 * 86400);
                    break;
                case "30d":
                    startTime = endTime.minusSeconds(30 * 86400);
                    break;
                case "all":
                    startTime = null; // No time limit
                    break;
                default:
                    startTime = endTime.minusSeconds(3600); // Default 1 hour
            }
        } else {
            startTime = endTime.minusSeconds(3600);
        }

        // Query with filters
        List<IdocEventDto> events = queryService.queryEventsFiltered(
            startTime,
            endTime,
            idocType,
            eventType,
            status,
            limit
        );

        // Build JSON response
        StringBuilder json = new StringBuilder("{\"total\":" + events.size() + ",\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            IdocEventDto event = events.get(i);
            if (i > 0) json.append(",");
            json.append(String.format(
                "{\"time\":\"%s\",\"eventType\":\"%s\",\"idocType\":\"%s\"," +
                "\"docNum\":\"%s\",\"status\":\"%s\",\"processingTime\":%d}",
                event.timestamp,
                event.eventType != null ? event.eventType : "",
                event.idocType != null ? event.idocType : "",
                event.docNum != null ? event.docNum : "",
                event.status != null ? event.status : "UNKNOWN",
                event.processingTimeMs
            ));
        }
        json.append("]}");

        resp.getWriter().write(json.toString());
    }

    private void handleIdocTypesQuery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<String> types = queryService.getUniqueIdocTypes();

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(types.get(i)).append("\"");
        }
        json.append("]");

        resp.getWriter().write(json.toString());
    }

    private void handleDailyQuery(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String dateStr = req.getParameter("date");
        LocalDate date = LocalDate.parse(dateStr);

        DailySummaryDto summary = queryService.getDailySummary(date);

        if (summary != null) {
            String json = String.format(
                "{\"date\":\"%s\",\"total\":%d,\"success\":%d,\"failed\":%d," +
                "\"avgProcessingTimeMs\":%d,\"totalXmlBytes\":%d,\"totalJsonBytes\":%d}",
                summary.date, summary.total, summary.success, summary.failed,
                summary.avgProcessingTimeMs, summary.totalXmlBytes, summary.totalJsonBytes
            );
            resp.getWriter().write(json);
        } else {
            resp.setStatus(404);
            resp.getWriter().write("{\"error\":\"No data found for date\"}");
        }
    }
}

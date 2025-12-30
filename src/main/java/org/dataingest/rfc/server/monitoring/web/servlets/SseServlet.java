package org.dataingest.rfc.server.monitoring.web.servlets;

import org.dataingest.rfc.server.config.IDocCaptureConfig;
import org.dataingest.rfc.server.monitoring.MetricsSnapshot;
import org.dataingest.rfc.server.monitoring.MetricsStore;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Server-Sent Events (SSE) servlet for real-time dashboard updates
 */
public class SseServlet extends HttpServlet {
    private final MetricsStore metricsStore;
    private final IDocCaptureConfig config;
    private final List<AsyncContext> clients = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public SseServlet(MetricsStore metricsStore, IDocCaptureConfig config) {
        this.metricsStore = metricsStore;
        this.config = config;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(0);  // No timeout
        clients.add(asyncContext);

        asyncContext.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                clients.remove(asyncContext);
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                clients.remove(asyncContext);
            }

            @Override
            public void onError(AsyncEvent event) {
                clients.remove(asyncContext);
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
            }
        });
    }

    public void start() {
        // Send metrics update every 1 second
        scheduler.scheduleAtFixedRate(() -> {
            try {
                MetricsSnapshot snapshot = metricsStore.getSnapshot();
                String json = snapshot.toJson();

                String sseMessage = "event: metric_update\n" +
                                   "data: " + json + "\n\n";

                broadcast(sseMessage);
            } catch (Exception e) {
                config.logError("[Monitoring] Error broadcasting metrics", e);
            }
        }, 1, 1, TimeUnit.SECONDS);

        config.log("[Monitoring] SSE broadcasting started");
    }

    public void broadcastIdocReceived(String idocType, String docNum) {
        String json = String.format(
            "{\"type\":\"%s\",\"docNum\":\"%s\",\"time\":\"%s\"}",
            idocType, docNum, java.time.Instant.now()
        );

        String sseMessage = "event: idoc_received\n" +
                           "data: " + json + "\n\n";

        broadcast(sseMessage);
    }

    public void broadcastError(String stage, String idocType, String message) {
        String json = String.format(
            "{\"stage\":\"%s\",\"idocType\":\"%s\",\"message\":\"%s\",\"time\":\"%s\"}",
            stage, idocType, message.replace("\"", "\\\""), java.time.Instant.now()
        );

        String sseMessage = "event: error_occurred\n" +
                           "data: " + json + "\n\n";

        broadcast(sseMessage);
    }

    private void broadcast(String message) {
        for (AsyncContext client : clients) {
            try {
                PrintWriter writer = client.getResponse().getWriter();
                writer.write(message);
                writer.flush();
            } catch (IOException e) {
                clients.remove(client);
            }
        }
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        for (AsyncContext client : clients) {
            try {
                client.complete();
            } catch (Exception e) {
                // Ignore
            }
        }
        clients.clear();

        config.log("[Monitoring] SSE broadcasting stopped");
    }
}

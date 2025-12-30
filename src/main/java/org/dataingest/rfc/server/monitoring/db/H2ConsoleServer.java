package org.dataingest.rfc.server.monitoring.db;

import org.dataingest.rfc.server.config.IDocCaptureConfig;
import org.h2.tools.Server;

/**
 * H2 Database Web Console Server
 */
public class H2ConsoleServer {
    private Server webServer;
    private final IDocCaptureConfig config;

    public H2ConsoleServer(IDocCaptureConfig config) {
        this.config = config;
    }

    public void start() throws Exception {
        if (!config.isH2ConsoleEnabled()) {
            return;
        }

        int port = config.getH2ConsolePort();

        // Start H2 web console
        webServer = Server.createWebServer(
            "-web",
            "-webAllowOthers",  // Allow remote connections
            "-webPort", String.valueOf(port)
        );

        webServer.start();

        config.log("[H2 Console] Started at http://localhost:" + port);
        config.log("[H2 Console] JDBC URL: jdbc:h2:file:./data/idoc_monitoring");
        config.log("[H2 Console] Username: sa");
        config.log("[H2 Console] Password: (blank)");
    }

    public void stop() {
        if (webServer != null && webServer.isRunning(false)) {
            webServer.stop();
            config.log("[H2 Console] Stopped");
        }
    }

    public boolean isRunning() {
        return webServer != null && webServer.isRunning(false);
    }
}

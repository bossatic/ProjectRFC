package org.dataingest.rfc.server.idoc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.dataingest.rfc.server.config.IDocCaptureConfig;

/**
 * Manages IDoc documentation files
 * Checks for documentation availability and returns file paths
 */
public class IDocDocumentationManager {

    private String documentationDirectory;
    private Map<String, String> documentationCache;
    private IDocCaptureConfig config;
    private Thread reloadWatcherThread;
    private volatile boolean isRunning = true;

    public IDocDocumentationManager(IDocCaptureConfig config) {
        this.config = config;
        this.documentationDirectory = config.getIdocDocumentationDirectory();
        this.documentationCache = new HashMap<>();

        // Create documentation directory if it doesn't exist
        File docDir = new File(documentationDirectory);
        if (!docDir.exists()) {
            if (docDir.mkdirs()) {
                config.log("Created documentation directory: " + documentationDirectory);
            }
        }

        // Scan for available documentation
        scanDocumentation();

        // Start background reload watcher if enabled
        int reloadInterval = config.getDocumentationReloadInterval();
        if (reloadInterval > 0) {
            startReloadWatcher(reloadInterval);
        }
    }

    /**
     * Scan documentation directory and cache available files
     */
    private void scanDocumentation() {
        File docDir = new File(documentationDirectory);

        config.log("Scanning documentation directory: " + documentationDirectory);

        if (!docDir.exists()) {
            config.log("Documentation directory does not exist yet: " + documentationDirectory);
            return;
        }

        if (!docDir.isDirectory()) {
            config.logError("Documentation path is not a directory: " + documentationDirectory, null);
            return;
        }

        File[] files = docDir.listFiles();
        if (files == null || files.length == 0) {
            config.log("Documentation directory is empty");
            return;
        }

        config.log("Found " + files.length + " files in documentation directory");

        for (File file : files) {
            if (file.isFile()) {
                String fileName = file.getName();
                // Extract IDoc type from filename (e.g., ORDERS05.doc, ORDERS05.txt -> ORDERS05)
                if (fileName.contains(".")) {
                    String idocType = fileName.substring(0, fileName.lastIndexOf('.')).toUpperCase();
                    documentationCache.put(idocType, file.getAbsolutePath());
                    config.log("  Loaded documentation for type: " + idocType + " (" + file.getName() + ")");
                }
            }
        }

        config.log("Documentation scan complete. Found " + documentationCache.size() + " documentation files available");
        if (documentationCache.size() > 0) {
            config.log("Available IDoc types: " + String.join(", ", documentationCache.keySet()));
        }
    }

    /**
     * Check if documentation exists for given IDoc type
     */
    public boolean hasDocumentation(String idocType) {
        return documentationCache.containsKey(idocType);
    }

    /**
     * Get documentation file path for given IDoc type
     */
    public String getDocumentationPath(String idocType) {
        return documentationCache.get(idocType);
    }

    /**
     * List all available documentation
     */
    public Map<String, String> getAvailableDocumentation() {
        return new HashMap<>(documentationCache);
    }

    /**
     * Rescan documentation directory (in case new files were added)
     */
    public void rescanDocumentation() {
        documentationCache.clear();
        scanDocumentation();
    }

    /**
     * Start background watcher thread to reload documentation periodically
     */
    private void startReloadWatcher(int intervalSeconds) {
        reloadWatcherThread = new Thread(() -> {
            config.log("Documentation watcher started (reload interval: " + intervalSeconds + "s)");

            while (isRunning) {
                try {
                    Thread.sleep(intervalSeconds * 1000L);

                    if (isRunning) {
                        config.log("Checking for new/updated documentation files...");
                        rescanDocumentation();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    config.logError("Error in documentation watcher thread", e);
                }
            }

            config.log("Documentation watcher stopped");
        });

        reloadWatcherThread.setName("IDocDocumentationWatcher");
        reloadWatcherThread.setDaemon(true);
        reloadWatcherThread.start();
    }

    /**
     * Stop the background watcher thread
     */
    public void stopReloadWatcher() {
        isRunning = false;
        if (reloadWatcherThread != null) {
            reloadWatcherThread.interrupt();
        }
    }
}

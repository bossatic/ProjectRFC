package org.dataingest.rfc.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sap.conn.idoc.IDocDocumentList;
import com.sap.conn.idoc.IDocXMLProcessor;
import com.sap.conn.idoc.jco.JCoIDoc;
import com.sap.conn.idoc.jco.JCoIDocHandler;
import com.sap.conn.idoc.jco.JCoIDocHandlerFactory;
import com.sap.conn.idoc.jco.JCoIDocServer;
import com.sap.conn.idoc.jco.JCoIDocServerContext;
import com.sap.conn.jco.server.JCoServer;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerContextInfo;
import com.sap.conn.jco.server.JCoServerErrorListener;
import com.sap.conn.jco.server.JCoServerExceptionListener;
import com.sap.conn.jco.server.JCoServerTIDHandler;

/**
 * Simple IDoc Capture Program
 *
 * Receives IDocs from SAP and stores them as XML files on disk.
 * Based on SAP's IDocServerExample.
 */
public class SimpleIDocCapture {

    private static final String OUTPUT_DIR = "idocs"; // Directory to store captured IDocs
    private static final String SERVER_NAME = "IDOC_SERVER"; // SAP server configuration name

    public static void main(String[] args) {
        try {
            // Create output directory if it doesn't exist
            File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
                System.out.println("Created output directory: " + outputDir.getAbsolutePath());
            }

            System.out.println("=== Simple IDoc Capture Server ===");
            System.out.println("Output directory: " + outputDir.getAbsolutePath());
            System.out.println("Server name: " + SERVER_NAME);
            System.out.println();

            // Get the IDoc server from JCo configuration
            JCoIDocServer server = JCoIDoc.getServer(SERVER_NAME);

            // Set up handlers
            server.setIDocHandlerFactory(new SimpleIDocHandlerFactory());
            server.setTIDHandler(new SimpleTIDHandler());

            // Set up error and exception listeners
            SimpleErrorListener listener = new SimpleErrorListener();
            server.addServerErrorListener(listener);
            server.addServerExceptionListener(listener);

            // Start the server
            System.out.println("Starting IDoc server...");
            server.start();
            System.out.println("✓ IDoc server started successfully!");
            System.out.println("Waiting for IDocs... (Press Ctrl+C to stop)");

        } catch (Exception e) {
            System.err.println("ERROR: Failed to start IDoc server");
            e.printStackTrace();
        }
    }

    /**
     * Handler that receives and processes incoming IDocs
     */
    static class SimpleIDocReceiveHandler implements JCoIDocHandler {

        @Override
        public void handleRequest(JCoServerContext serverCtx, IDocDocumentList idocList) {
            FileOutputStream fos = null;
            OutputStreamWriter osw = null;

            try {
                String tid = serverCtx.getTID();
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String filename = OUTPUT_DIR + File.separator + timestamp + "_" + tid + ".xml";

                System.out.println("\n→ Received IDoc(s):");
                System.out.println("  Transaction ID: " + tid);
                System.out.println("  Number of IDocs: " + idocList.size());
                System.out.println("  Saving to: " + filename);

                // Create XML processor to convert IDoc to XML format
                IDocXMLProcessor xmlProcessor = JCoIDoc.getIDocFactory().getIDocXMLProcessor();

                // Write IDoc to file
                fos = new FileOutputStream(filename);
                osw = new OutputStreamWriter(fos, "UTF-8");
                xmlProcessor.render(idocList, osw, IDocXMLProcessor.RENDER_WITH_TABS_AND_CRLF);
                osw.flush();

                System.out.println("  ✓ IDoc saved successfully!");

            } catch (Throwable thr) {
                System.err.println("  ✗ ERROR saving IDoc:");
                thr.printStackTrace();
            } finally {
                // Clean up resources
                try {
                    if (osw != null) osw.close();
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Factory that creates IDoc handlers
     */
    static class SimpleIDocHandlerFactory implements JCoIDocHandlerFactory {

        private JCoIDocHandler handler = new SimpleIDocReceiveHandler();

        @Override
        public JCoIDocHandler getIDocHandler(JCoIDocServerContext serverCtx) {
            return handler;
        }
    }

    /**
     * Transaction ID handler for tracking IDoc processing
     */
    static class SimpleTIDHandler implements JCoServerTIDHandler {

        @Override
        public boolean checkTID(JCoServerContext serverCtx, String tid) {
            System.out.println("  checkTID: " + tid);
            return true; // Accept all TIDs
        }

        @Override
        public void confirmTID(JCoServerContext serverCtx, String tid) {
            System.out.println("  confirmTID: " + tid);
        }

        @Override
        public void commit(JCoServerContext serverCtx, String tid) {
            System.out.println("  commit: " + tid);
        }

        @Override
        public void rollback(JCoServerContext serverCtx, String tid) {
            System.out.println("  rollback: " + tid);
        }
    }

    /**
     * Error and exception listener
     */
    static class SimpleErrorListener implements JCoServerErrorListener, JCoServerExceptionListener {

        @Override
        public void serverErrorOccurred(JCoServer server, String connectionId,
                                       JCoServerContextInfo ctx, Error error) {
            System.err.println("\n!!! Server Error on " + server.getProgramID() +
                             " connection " + connectionId);
            error.printStackTrace();
        }

        @Override
        public void serverExceptionOccurred(JCoServer server, String connectionId,
                                           JCoServerContextInfo ctx, Exception error) {
            System.err.println("\n!!! Server Exception on " + server.getProgramID() +
                             " connection " + connectionId);
            error.printStackTrace();
        }
    }
}

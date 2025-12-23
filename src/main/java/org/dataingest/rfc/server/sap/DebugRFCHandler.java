package org.dataingest.rfc.server.sap;

import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.AbapClassException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Debug RFC handler that logs ALL function calls to help diagnose issues.
 * This is a minimal handler that just logs and returns success.
 */
@Component
public class DebugRFCHandler implements JCoServerFunctionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DebugRFCHandler.class);

    @Override
    public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
            throws AbapException, AbapClassException {
        LOGGER.error("!!!!! DEBUG HANDLER CALLED !!!!!");
        LOGGER.error("Function name: {}", function.getName());
        LOGGER.error("TID: {}", serverCtx.getTID());
        LOGGER.error("System: {}", serverCtx.getConnectionAttributes().getSystemID());
        LOGGER.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
}

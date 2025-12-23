package org.dataingest.rfc.server.sap;

import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.server.JCoServerContext;
import com.sap.conn.jco.server.JCoServerFunctionHandler;
import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.AbapClassException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Universal RFC handler that accepts ANY function name.
 * This helps debug what functions SAP is actually calling.
 */
public class UniversalRFCHandler implements JCoServerFunctionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalRFCHandler.class);
    private final JCoServerFunctionHandler delegate;
    private final String delegateName;

    public UniversalRFCHandler(JCoServerFunctionHandler delegate, String delegateName) {
        this.delegate = delegate;
        this.delegateName = delegateName;
    }

    @Override
    public void handleRequest(JCoServerContext serverCtx, JCoFunction function)
            throws AbapException, AbapClassException {
        LOGGER.error("!!!!! UNIVERSAL HANDLER CALLED !!!!!");
        LOGGER.error("Function: {}", function.getName());
        LOGGER.error("Delegate: {}", delegateName);
        LOGGER.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        if (delegate != null) {
            delegate.handleRequest(serverCtx, function);
        }
    }
}

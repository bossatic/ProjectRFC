/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.talend.sap.idoc.ISAPIDocReceiver
 *  org.talend.sap.impl.server.SAPIDocReceiverMock
 */
package org.talend.sap.server;

import org.talend.sap.idoc.ISAPIDocReceiver;
import org.talend.sap.impl.server.SAPIDocReceiverMock;

public interface SAPIDocReceiverFactory {
    public ISAPIDocReceiver create();

    public SAPIDocReceiverMock createMock(String var1);
}

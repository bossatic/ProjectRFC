/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Qualifier
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Component
 *  org.talend.sap.idoc.ISAPIDocPackage
 *  org.talend.sap.idoc.ISAPIDocReceiver
 *  org.talend.sap.impl.idoc.SAPIDocReceiver
 *  org.talend.sap.impl.idoc.SAPIDocTransaction
 *  org.talend.sap.server.SAPIDocReceiverFactory
 */
package org.talend.sap.impl.server;

import java.util.concurrent.BlockingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.talend.sap.idoc.ISAPIDocPackage;
import org.talend.sap.idoc.ISAPIDocReceiver;
import org.talend.sap.impl.idoc.SAPIDocReceiver;
import org.talend.sap.impl.idoc.SAPIDocTransaction;
import org.talend.sap.impl.server.SAPIDocReceiverMock;
import org.talend.sap.server.SAPIDocReceiverFactory;

@Component
public class SAPIDocReceiverFactoryImpl
implements SAPIDocReceiverFactory {
    @Value(value="${feature.idoc.transactional:false}")
    protected boolean transactional;
    @Value(value="${feature.idoc.transactionAbortTimeout:60000}")
    protected long abortTimeout;
    @Autowired
    @Qualifier(value="idocQueue")
    protected BlockingQueue<ISAPIDocPackage> idocQueue;

    public ISAPIDocReceiver create() {
        if (this.transactional) {
            return new SAPIDocTransaction(this.abortTimeout, this.idocQueue);
        }
        return new SAPIDocReceiver(this.idocQueue);
    }

    public SAPIDocReceiverMock createMock(String connectionId) {
        return new SAPIDocReceiverMock(connectionId, this.idocQueue);
    }
}

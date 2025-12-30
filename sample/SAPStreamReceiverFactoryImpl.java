/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Component
 *  org.talend.sap.impl.stream.SAPStreamReceiver
 *  org.talend.sap.server.SAPStreamReceiverFactory
 *  org.talend.sap.stream.ISAPStreamReceiver
 *  org.talend.sap.stream.ISAPStreamReceiverService
 */
package org.talend.sap.impl.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.talend.sap.impl.stream.SAPStreamReceiver;
import org.talend.sap.server.SAPStreamReceiverFactory;
import org.talend.sap.stream.ISAPStreamReceiver;
import org.talend.sap.stream.ISAPStreamReceiverService;

@Component
public class SAPStreamReceiverFactoryImpl
implements SAPStreamReceiverFactory {
    @Value(value="${feature.streaming.timeout:30000}")
    protected long timeout;
    @Autowired
    protected ISAPStreamReceiverService streamReceiverService;

    public ISAPStreamReceiver create() {
        return new SAPStreamReceiver(this.streamReceiverService, this.timeout);
    }
}

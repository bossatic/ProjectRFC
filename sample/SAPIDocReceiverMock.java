/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.talend.sap.idoc.ISAPIDoc
 *  org.talend.sap.idoc.ISAPIDocPackage
 *  org.talend.sap.idoc.ISAPIDocReceiver
 *  org.talend.sap.idoc.ISAPIDocSupport
 *  org.talend.sap.impl.idoc.SAPIDocPackage
 *  org.talend.sap.impl.idoc.SAPIDocSupport
 */
package org.talend.sap.impl.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.talend.sap.idoc.ISAPIDoc;
import org.talend.sap.idoc.ISAPIDocPackage;
import org.talend.sap.idoc.ISAPIDocReceiver;
import org.talend.sap.idoc.ISAPIDocSupport;
import org.talend.sap.impl.idoc.SAPIDocPackage;
import org.talend.sap.impl.idoc.SAPIDocSupport;

public class SAPIDocReceiverMock
implements ISAPIDocReceiver,
Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SAPIDocReceiverMock.class);
    private static final long TIMEOUT = 5000L;
    @Value(value="${feature.idoc.transactional:false}")
    protected boolean transactional;
    private final String connectionId;
    private final BlockingQueue<ISAPIDocPackage> idocQueue;
    private final List<ISAPIDoc> idocs;
    private int tid;

    public SAPIDocReceiverMock(String connectionId, BlockingQueue<ISAPIDocPackage> idocQueue) {
        this.connectionId = connectionId;
        this.idocQueue = idocQueue;
        this.idocs = new ArrayList<ISAPIDoc>(4);
        this.idocs.add(this.loadFromResource("sample-idocs/0000000000813429.txt"));
        this.idocs.add(this.loadFromResource("sample-idocs/0000000000813429.txt"));
        this.idocs.add(this.loadFromResource("sample-idocs/0000000000814496.txt"));
        this.idocs.add(this.loadFromResource("sample-idocs/0000000000814490.txt"));
        this.tid = 0;
    }

    public String getName() {
        return null;
    }

    public boolean isTransactional() {
        return this.transactional;
    }

    public ISAPIDocPackage receive(long timeout) {
        try {
            TimeUnit.MILLISECONDS.sleep(timeout > 0L ? timeout : 5000L);
        }
        catch (InterruptedException e) {
            return null;
        }
        SAPIDocPackage idocPackage = new SAPIDocPackage(this.connectionId, String.valueOf(this.tid), this.idocs);
        LOGGER.info("{}: Receiving IDOC package with TID '{}'", (Object)this.connectionId, (Object)idocPackage.getTID());
        ++this.tid;
        return idocPackage;
    }

    @Override
    public void run() {
        while (true) {
            try {
                TimeUnit.MILLISECONDS.sleep(5000L);
            }
            catch (InterruptedException e) {
                return;
            }
            SAPIDocPackage idocPackage = new SAPIDocPackage(this.connectionId, String.valueOf(this.tid), this.idocs);
            LOGGER.info("{}: Receiving IDOC package with TID '{}'", (Object)this.connectionId, (Object)idocPackage.getTID());
            this.idocQueue.add((ISAPIDocPackage)idocPackage);
            ++this.tid;
        }
    }

    protected ISAPIDoc loadFromResource(String resourceName) {
        ISAPIDocSupport idocSupport = SAPIDocSupport.getInstance();
        try {
            return idocSupport.fromStream(this.getClass().getClassLoader().getResourceAsStream(resourceName), "UTF-8");
        }
        catch (IOException e) {
            throw new RuntimeException(String.format("IDOC could not be loaded from resource '%s'", resourceName), e);
        }
    }
}

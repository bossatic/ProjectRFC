/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.talend.sap.ISAPConnection
 *  org.talend.sap.ISAPServer
 */
package org.talend.sap.server.named;

import org.talend.sap.ISAPConnection;
import org.talend.sap.ISAPServer;
import org.talend.sap.server.named.SAPNamedConnectionFeature;

public interface SAPNamedConnection {
    public ISAPConnection getConnection();

    public String getId();

    public String getProperty(String var1);

    public ISAPServer getServer();

    public boolean hasProperty(String var1);

    public boolean isAtLeastOneFeatureEnabled(SAPNamedConnectionFeature ... var1);

    public boolean isFeatureEnabled(SAPNamedConnectionFeature var1);
}

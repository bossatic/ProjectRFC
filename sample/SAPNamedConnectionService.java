/*
 * Decompiled with CFR 0.152.
 */
package org.talend.sap.server.named;

import org.talend.sap.server.named.SAPNamedConnection;
import org.talend.sap.server.named.SAPNamedConnectionFeature;

public interface SAPNamedConnectionService {
    public SAPNamedConnection get(String var1);

    public boolean has(String var1);

    public boolean isFeaturePresent(SAPNamedConnectionFeature var1);
}

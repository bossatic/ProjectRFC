/*
 * Decompiled with CFR 0.152.
 */
package org.talend.sap.server.named;

public enum SAPNamedConnectionFeature {
    BW_SOURCE_SYSTEM("feature.bw_source_system.enabled"),
    BW_SOURCE_SYSTEM_MOCK("feature.bw_source_system.mock.enabled"),
    IDOC("feature.idoc.enabled"),
    IDOC_MOCK("feature.idoc.mock.enabled"),
    STREAMING("feature.streaming.enabled");

    private final String propertyName;

    private SAPNamedConnectionFeature(String propertyName) {
        this.propertyName = propertyName;
    }

    public String propertyName() {
        return this.propertyName;
    }
}

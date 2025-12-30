/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.talend.sap.idoc.ISAPIDoc
 */
package org.talend.sap.impl.server;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.talend.sap.idoc.ISAPIDoc;

public class SAPIDocNameUtil {
    private static final String IDOC_EXTENSION_NAME_SEP = ".";
    private static final Pattern STRUCTURE_NAME_INVALID_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9_]");
    private static final String JMS_RESOURCES_NAMESPACE = "TALEND.IDOCS.";

    public static String toIDocStructName(String name, String extensionName) {
        return SAPIDocNameUtil.toValidStructName(name) + (extensionName == null ? "" : IDOC_EXTENSION_NAME_SEP + SAPIDocNameUtil.toValidStructName(extensionName));
    }

    public static String toValidStructName(String hint) {
        if (hint == null) {
            return null;
        }
        return STRUCTURE_NAME_INVALID_CHARS_PATTERN.matcher(hint).replaceAll("_");
    }

    public static String toFriendlyName(ISAPIDoc idoc) {
        StringBuilder sb = new StringBuilder();
        sb.append(idoc.getType());
        if (idoc.getExtension() != null) {
            sb.append(IDOC_EXTENSION_NAME_SEP);
            sb.append(idoc.getExtension());
        }
        if (StringUtils.isNotBlank((CharSequence)idoc.getRelease())) {
            sb.append(" [");
            sb.append(idoc.getRelease());
            sb.append("]");
        }
        return sb.toString();
    }

    public static String getTopicName(ISAPIDoc idoc) {
        return JMS_RESOURCES_NAMESPACE + SAPIDocNameUtil.toIDocStructName(idoc.getType(), idoc.getExtension());
    }

    public static String getQueueName(ISAPIDoc idoc) {
        return JMS_RESOURCES_NAMESPACE + SAPIDocNameUtil.toIDocStructName(idoc.getType(), idoc.getExtension());
    }

    public static String getIDocFriendlyName(ISAPIDoc idoc) {
        return SAPIDocNameUtil.toFriendlyName(idoc);
    }

    private SAPIDocNameUtil() {
    }
}

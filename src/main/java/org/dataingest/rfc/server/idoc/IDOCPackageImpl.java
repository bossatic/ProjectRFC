package org.dataingest.rfc.server.idoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dataingest.rfc.server.model.SAPIDOCDocument;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of IIDOCPackage using SAP JCo.
 *
 * Represents a package of IDOCs received from SAP with transaction information.
 * The actual transaction management (commit/rollback) is handled by the SAP JCo
 * TID handler framework, which calls confirmTID(), commit(), and rollback() methods
 * on the registered TID handler.
 */
public class IDOCPackageImpl implements IIDOCPackage {

    private static final Logger LOGGER = LoggerFactory.getLogger(IDOCPackageImpl.class);

    private final String tid;
    private final String partnerHost;
    private final List<SAPIDOCDocument> idocs;

    /**
     * Constructs an IDOC package.
     *
     * @param tid the transaction ID
     * @param partnerHost the sending SAP system identifier
     * @param idocs the list of IDOC documents
     */
    public IDOCPackageImpl(
            String tid,
            String partnerHost,
            List<SAPIDOCDocument> idocs) {
        this.tid = tid;
        this.partnerHost = partnerHost;
        this.idocs = new ArrayList<>(idocs);
    }

    @Override
    public String getTID() {
        return tid;
    }

    @Override
    public String getPartnerHost() {
        return partnerHost;
    }

    @Override
    public List<SAPIDOCDocument> getIdocs() {
        return new ArrayList<>(idocs);
    }

    @Override
    public int size() {
        return idocs.size();
    }

    @Override
    public SAPIDOCDocument get(int index) {
        return idocs.get(index);
    }

    /**
     * Marks this IDOC package for commit.
     *
     * The actual SAP transaction management is handled by the TID handler framework.
     * This method indicates successful processing to enable SAP tRFC confirmation.
     *
     * Transaction flow:
     * 1. SAP sends IDOC with TID
     * 2. RFC handler processes IDOC
     * 3. Handler calls commit() to indicate success
     * 4. SAP TID handler confirms the transaction
     */
    @Override
    public void commit() throws Exception {
        LOGGER.info("╔═══════════════════════════════════════════════════════════╗");
        LOGGER.info("║         IDOC PACKAGE COMMIT - TID: {}                    ║", tid);
        LOGGER.info("╚═══════════════════════════════════════════════════════════╝");
        LOGGER.info("IDOC package marked for commit. TID: {}, IDOCs: {}", tid, idocs.size());
        LOGGER.info("✓ SAP TID handler will confirm this transaction");
    }

    /**
     * Marks this IDOC package for rollback.
     *
     * The actual SAP transaction management is handled by the TID handler framework.
     * This method indicates failure to enable SAP tRFC rollback.
     *
     * @param reason the reason for rollback
     */
    @Override
    public void rollback(String reason) throws Exception {
        LOGGER.info("╔═══════════════════════════════════════════════════════════╗");
        LOGGER.info("║       IDOC PACKAGE ROLLBACK - TID: {}                    ║", tid);
        LOGGER.info("╚═══════════════════════════════════════════════════════════╝");
        LOGGER.warn("IDOC package marked for rollback. TID: {}, Reason: {}", tid, reason);
        LOGGER.info("⚠️  SAP TID handler will rollback this transaction");
    }
}

package org.dataingest.rfc.server.idoc;

import org.dataingest.rfc.server.model.SAPIDOCDocument;
import java.util.List;

/**
 * Interface representing a package of IDOCs received from SAP.
 *
 * Analogous to Talend's ISAPIDocPackage but implemented using only SAP JCo.
 * Contains IDOC data and transaction management methods.
 */
public interface IIDOCPackage {

    /**
     * Gets the transaction ID (TID) for this IDOC package.
     * Used for SAP tRFC transaction management.
     *
     * @return the TID string
     */
    String getTID();

    /**
     * Gets the sending SAP system identifier.
     *
     * @return the SAP system ID (e.g., "HD1")
     */
    String getPartnerHost();

    /**
     * Gets the list of IDOCs in this package.
     *
     * @return list of IDOC documents
     */
    List<SAPIDOCDocument> getIdocs();

    /**
     * Gets the number of IDOCs in this package.
     *
     * @return the count of IDOCs
     */
    int size();

    /**
     * Gets the IDOC at the specified index.
     *
     * @param index the index
     * @return the IDOC document
     */
    SAPIDOCDocument get(int index);

    /**
     * Commits the IDOC package transaction with SAP.
     * Indicates to SAP that the IDOCs were successfully processed.
     *
     * @throws Exception if commit fails
     */
    void commit() throws Exception;

    /**
     * Rolls back the IDOC package transaction with SAP.
     * Indicates to SAP that the IDOCs failed to process.
     *
     * @param reason the reason for rollback
     * @throws Exception if rollback fails
     */
    void rollback(String reason) throws Exception;
}

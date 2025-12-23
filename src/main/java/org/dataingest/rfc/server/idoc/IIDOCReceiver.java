package org.dataingest.rfc.server.idoc;

/**
 * Interface for receiving IDOC packages from SAP.
 *
 * Analogous to Talend's ISAPIDocReceiver but implemented using only SAP JCo.
 * Handles reception and transaction management of IDOC data.
 */
public interface IIDOCReceiver {

    /**
     * Gets the name/identifier of this receiver.
     *
     * @return the receiver name
     */
    String getName();

    /**
     * Indicates whether this receiver supports transactional processing.
     *
     * @return true if transactional, false otherwise
     */
    boolean isTransactional();

    /**
     * Receives an IDOC package from SAP.
     *
     * This method is called when SAP sends IDOC data via RFC.
     * The implementation should extract IDOC data from the RFC call
     * and create an IIDOCPackage object.
     *
     * @param idocPackage the IDOC package containing SAP data
     * @throws Exception if reception or processing fails
     */
    void receiveIdoc(IIDOCPackage idocPackage) throws Exception;
}

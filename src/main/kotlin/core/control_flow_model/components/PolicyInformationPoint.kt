package core.control_flow_model.components

/**
 * LUCE PIP, as defined in LUCE's control flow model (see Section 6.1.1)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyInformationPoint {

    /**
     * Query a piece of information, identified by the given identifier, from the PIP
     *
     * @return null when the information is not available, the information of type Any otherwise
     */
    fun queryInformation(identifier: Any) : Any?

    /**
     * Update a specific piece of information, identified by the given identifier, at the PIP
     *
     * - description argument: can be used to describe the update actions (e.g. increment a ctr)
     * - value argument: can be used to calculate / overwrite the information
     *
     * @return true on success
     */
    fun updateInformation(identifier: Any, description: String, value: Any?) : Boolean

}
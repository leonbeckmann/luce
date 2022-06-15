package core.control_flow_model.components

import core.control_flow_model.messages.RevocationResponse

/**
 * LUCE PEP, as defined in LUCE's control flow model (see Section 6.1.1)
 *
 * Here, the PEP is a callback interface for asynchronous events from the PDP to the PEP.
 * All synchronous event, initiated by the PEP, are custom implementations.
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyEnforcementPoint {

    /**
     * Called asynchronously on revocation by the PDP
     */
    fun onRevocation(response: RevocationResponse)

    /**
     * Called asynchronously when dependency is required
     */
    fun doDependency(dependencyId: String) : Boolean

}
package core.control_flow_model.components

import core.control_flow_model.messages.RevocationResponse

/**
 * LUCE PEP
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
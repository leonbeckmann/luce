package core.control_flow_model.components

import core.control_flow_model.messages.RevocationResponse

/**
 * LUCE PEP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyEnforcementPoint {

    fun onRevocation(response: RevocationResponse)

}
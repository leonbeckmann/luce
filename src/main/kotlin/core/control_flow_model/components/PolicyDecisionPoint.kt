package core.control_flow_model.components

import core.LuceConfiguration
import core.control_flow_model.messages.DecisionRequest
import core.control_flow_model.messages.DecisionResponse

/**
 * Policy Decision Point (PDP)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
object PolicyDecisionPoint {

    private lateinit var policyManagementPoint: PolicyManagementPoint


    fun configure(configuration: LuceConfiguration) {
        this.policyManagementPoint = configuration.policyManagementPoint
    }

    fun <Sid, Oid> requestDecision(request: DecisionRequest<Sid, Oid>) : DecisionResponse {

        // pull policy from PMP
        val policy = policyManagementPoint.pull()

        // TODO usage decision process

        return DecisionResponse()
    }

}
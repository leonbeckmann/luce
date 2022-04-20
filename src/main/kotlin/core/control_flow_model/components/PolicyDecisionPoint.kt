package core.control_flow_model.components

import core.control_flow_model.messages.DecisionRequest
import core.control_flow_model.messages.DecisionResponse

/**
 * Policy Decision Point (PDP)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
object PolicyDecisionPoint {

    fun requestDecision(request: DecisionRequest) : DecisionResponse {

        // TODO usage decision process

        return DecisionResponse()
    }

}
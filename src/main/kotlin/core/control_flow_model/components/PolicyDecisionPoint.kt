package core.control_flow_model.components

import core.control_flow_model.messages.DecisionRequest
import core.control_flow_model.messages.DecisionResponse

/**
 * LUCE PDP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class PolicyDecisionPoint {

    companion object {

        /**
         * PDP logic: PEP requests PDP decision based on policy evaluation.
         *
         * Calling this function corresponds with the tryAccess action.
         *
         * @return PDP decision
         */
        fun <Sid, Oid> requestDecision(request: DecisionRequest<Sid, Oid>) : DecisionResponse {

            // TODO initiate fresh usage session at the session PIP, session must not be active yet

            // TODO get policy from PMP

            // TODO evaluate policy

            // TODO on success, bind policy to session

            // TODO unlock session

            // TODO return decision
            return DecisionResponse()
        }
    }
}
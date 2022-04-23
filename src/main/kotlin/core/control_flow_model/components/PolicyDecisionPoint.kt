package core.control_flow_model.components

import core.control_flow_model.messages.DecisionRequest
import core.control_flow_model.messages.DecisionResponse
import core.exceptions.LuceException
import core.usage_decision_process.UsageSession

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

            // initiate fresh usage session at the session PIP, session must not be active yet
            val sessionPip = ComponentRegistry.policyInformationPoints["usage_session"] ?:
                throw LuceException("PIP for usage sessions not available")

            val sessionId =
                request.luceObject.identity.toString() + request.luceSubject.identity.toString() + request.luceRight.id

            val session = sessionPip.queryInformation(sessionId) as UsageSession
            if (session.state != UsageSession.State.Initial) throw LuceException("Usage session not in initial state")

            // get policy from PMP
            val policy = ComponentRegistry.policyManagementPoint.pullPolicy() ?:
                throw LuceException("Policy is missing")
            
            // TODO evaluate policy

            // TODO on success, bind policy to session

            // unlock session
            session.unlock()

            // TODO fill decision
            return DecisionResponse()
        }
    }
}
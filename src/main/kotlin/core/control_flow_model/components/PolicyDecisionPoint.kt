package core.control_flow_model.components

import core.control_flow_model.messages.DecisionRequest
import core.control_flow_model.messages.DecisionResponse
import core.exceptions.LuceException
import core.logic.PolicyEvaluator
import core.usage_decision_process.UsageSession
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.dsl.prolog
import it.unibo.tuprolog.solve.Solution
import it.unibo.tuprolog.solve.SolveOptions

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
            val solution = PolicyEvaluator.evaluate(
                prolog { Atom.of("Alice") and Atom.of("Bob") },
                SolveOptions.DEFAULT
            )

            // TODO act according to result
            when (solution) {
                is Solution.Yes -> {
                    // TODO on success, bind policy to session
                }
                is Solution.No -> {}
                is Solution.Halt -> {}
            }

            // unlock session
            session.unlock()

            // TODO fill decision
            return DecisionResponse()
        }
    }
}
package core.control_flow_model.components

import core.control_flow_model.messages.DecisionRequest
import core.control_flow_model.messages.DecisionResponse
import core.exceptions.LuceException
import core.logic.PolicyEvaluator
import core.usage_decision_process.UsageSession
import it.unibo.tuprolog.solve.Solution
import it.unibo.tuprolog.solve.SolveOptions
import org.slf4j.LoggerFactory

/**
 * LUCE PDP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class PolicyDecisionPoint {

    companion object {
        private val LOG = LoggerFactory.getLogger(PolicyDecisionPoint::class.java)

        /**
         * PDP logic: PEP requests PDP decision based on policy evaluation.
         *
         * Calling this function corresponds with the tryAccess action.
         *
         * @return PDP decision
         */
        fun <Sid, Oid> requestDecision(request: DecisionRequest<Sid, Oid>) : DecisionResponse {

            if (LOG.isDebugEnabled) {
                LOG.debug("Request decision for " +
                        "subject=${request.luceSubject.identity}, " +
                        "object=${request.luceObject.identity}, " +
                        "right=${request.luceRight.id}"
                )
            }

            // initiate fresh usage session at the session PIP, session must not be active yet
            val sessionPip = ComponentRegistry.policyInformationPoints["usage_session"] ?:
                throw LuceException("PIP for usage sessions not available")

            val sessionId =
                request.luceObject.identity.toString() + request.luceSubject.identity.toString() + request.luceRight.id

            val session = sessionPip.queryInformation(sessionId) as UsageSession
            if (session.state != UsageSession.State.Initial) throw LuceException("Usage session not in initial state")

            // feed tryAccess
            session.feedEvent(UsageSession.Event.TryAccess)

            // get policy from PMP
            val policy = ComponentRegistry.policyManagementPoint.pullPolicy() ?:
                throw LuceException("Policy is missing")

            if (LOG.isTraceEnabled) {
                LOG.trace("Retrieved policy=$policy from PMP")
                LOG.trace("Start pre-access policy evaluation")
            }

            // evaluate policy
            val solution = PolicyEvaluator.evaluate(
                policy.preAccess,
                SolveOptions.DEFAULT
            )

            // respond according to result
            when (solution) {
                is Solution.Yes -> {

                    if (LOG.isDebugEnabled) {
                        LOG.debug("Positive policy evaluation result - Permit the usage")
                    }

                    // on success, permit access and bind policy to session
                    session.feedEvent(UsageSession.Event.PermitAccess)
                    session.bindToPolicy(policy)

                    // TODO UR2

                    // unlock session for further usage
                    session.unlock()

                    // return positive decision
                    return DecisionResponse.PERMITTED
                }
                is Solution.No -> {

                    if (LOG.isDebugEnabled) {
                        LOG.debug("Negative policy evaluation result - Deny the usage")
                    }

                    // on failure, deny access and delete usage session
                    session.feedEvent(UsageSession.Event.DenyAccess)
                    sessionPip.updateInformation(sessionId, null)

                    // return negative decision
                    return DecisionResponse.DENIED
                }
                is Solution.Halt -> {

                    if (LOG.isDebugEnabled) {
                        LOG.debug("Policy evaluation failed with an exception=${solution.exception} - Deny the usage")
                    }

                    // on exception, deny access and delete usage session
                    session.feedEvent(UsageSession.Event.DenyAccess)
                    sessionPip.updateInformation(sessionId, null)

                    // return negative decision
                    return DecisionResponse.DENIED
                }
            }
        }
    }
}
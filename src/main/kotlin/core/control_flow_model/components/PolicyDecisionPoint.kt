package core.control_flow_model.components

import core.control_flow_model.messages.DecisionRequest
import core.control_flow_model.messages.DecisionResponse
import core.exceptions.LuceException
import core.logic.PolicyEvaluator
import core.usage_decision_process.SessionPip.SessionIdentifier
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

        // TODO improve Session State handling

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

            // initiate fresh usage session at the session PIP, session must not be active yet, i.e. in initial state
            val sessionId =
                request.luceObject.identity.toString() + request.luceSubject.identity.toString() + request.luceRight.id

            // TODO catch exception for invalid state and return negative response 'already in use'
            val session = getSession(sessionId, UsageSession.State.Initial)

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

                    // start reevaluation timer (if configured) for ongoing usage decisions
                    if (policy.ongoingPeriod != null) {
                        val timer = ReevaluationTimer(policy.ongoingPeriod, policy.ongoingPeriod, sessionId)
                        timer.schedule()
                        session.reevaluationTimer = timer
                    }

                    // unlock session for further usage
                    session.unlock()

                    // return positive decision
                    return DecisionResponse.PERMITTED
                }
                else -> {
                    if (solution is Solution.Halt) {
                        // failure with exception
                        if (LOG.isDebugEnabled) {
                            LOG.debug("Policy evaluation failed with an exception=${solution.exception} - Deny the usage")
                        }
                    } else {
                        // negative prolog decision
                        if (LOG.isDebugEnabled) {
                            LOG.debug("Negative policy evaluation result - Deny the usage")
                        }
                    }

                    // on failure, deny access and delete usage session
                    session.feedEvent(UsageSession.Event.DenyAccess)
                    getPip("usage_session").updateInformation(sessionId, null)

                    // return negative decision
                    return DecisionResponse.DENIED
                }
            }
        }

        /**
         * PDP logic: ReevaluationTimer triggers policy re-evaluation.
         *
         * Calling this function corresponds with ongoing action.
         */
        fun triggerPeriodic(sessionId: String) {
            if (LOG.isTraceEnabled) {
                LOG.trace("Usage decision triggered by reevaluation timer for session with id=$sessionId")
            }

            // get session and assert state = accessing
            val session = getSession(sessionId, UsageSession.State.Accessing)
            val policy = session.policy ?: throw LuceException("Missing policy for ongoing session=$sessionId")

            // re-evaluate policy
            if (LOG.isTraceEnabled) {
                LOG.trace("Retrieved policy=$policy from session")
                LOG.trace("Start ongoing-access policy evaluation")
            }

            // evaluate policy
            val solution = PolicyEvaluator.evaluate(
                policy.ongoingAccess,
                SolveOptions.DEFAULT
            )

            when (solution) {
                is Solution.Yes -> {
                    if (LOG.isDebugEnabled) {
                        LOG.debug("Positive policy re-evaluation result - Continue the usage")
                    }

                    // unlock session for further usage
                    session.unlock()
                }
                else -> {
                    if (solution is Solution.Halt) {
                        // failure with exception
                        if (LOG.isDebugEnabled) {
                            LOG.debug("Policy re-evaluation failed with an exception=${solution.exception} - Revoke the usage")
                        }
                    } else {
                        // negative prolog decision
                        if (LOG.isDebugEnabled) {
                            LOG.debug("Negative policy re-evaluation result - Revoke the usage")
                        }
                    }

                    // revoke the usage on failure
                    session.feedEvent(UsageSession.Event.RevokeAccess)
                    val revokeSolution = PolicyEvaluator.evaluate(
                        policy.postAccessRevoked,
                        SolveOptions.DEFAULT
                    )

                    if (LOG.isDebugEnabled){
                        LOG.debug("Revocation resulted in solution=$revokeSolution")
                    }

                    // delete session
                    getPip("usage_session").updateInformation(sessionId, null)
                }
            }
        }

        /**
         * PDP logic: End the usage synchronously, triggered by the subject.
         *
         * Calling this function corresponds with the endAccess action.
         */
        fun endUsage() {
            // TODO
        }

        private fun getPip(pipIdentifier: String): PolicyInformationPoint {
            return ComponentRegistry.policyInformationPoints[pipIdentifier]
                ?: throw LuceException("PIP with identifier=$pipIdentifier is not registered")
        }

        private fun getSession(sessionId: String, expectedState: UsageSession.State): UsageSession {

            // get session PIP
            val sessionPip = getPip("usage_session")

            // get session
            return sessionPip.queryInformation(SessionIdentifier(sessionId, expectedState)) as UsageSession
        }

    }
}
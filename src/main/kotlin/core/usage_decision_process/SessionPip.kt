package core.usage_decision_process

import core.control_flow_model.components.PolicyInformationPoint
import core.exceptions.LuceException
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Policy Information Point, providing usage session information to the PDP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class SessionPip : PolicyInformationPoint {

    data class SessionIdentifier(val sessionId : String, val expectedState: UsageSession.State?)

    private val sessions = ConcurrentHashMap<String, UsageSession>()

    /**
     * Called by the PDP, returns a (locked) usage session
     */
    override fun queryInformation(identifier: Any): UsageSession {

        if (identifier !is SessionIdentifier) {
            throw LuceException("Invalid identifier type in queryInformation: SessionIdentifier expected")
        }

        val id = identifier.sessionId
        val expectedState = identifier.expectedState!!

        if (LOG.isTraceEnabled) {
            LOG.trace("Query usage session with id=$id")
        }

        // first check if the usage session already exists, otherwise create a new one
        val session = when(expectedState) {
            UsageSession.State.Initial -> {
                // expect initial session, which might already exist but must be retested
                sessions.getOrPut(id) {
                    if (LOG.isTraceEnabled) {
                        LOG.trace("Put new usage session with id=$id")
                    }
                    UsageSession()
                }
            }
            else -> {
                // expect existent session
                sessions[id] ?: throw LuceException("Expected session with id=$id does not exist")
            }
        }

        // lock session to get synchronized access
        session.lock()

        // check expected state
        if (session.state != expectedState) {
            val state = session.state
            session.unlock()
            throw LuceException("Session with id=$id in state=${state} is not in expected " +
                    "state=$expectedState")
        }

        return session
    }

    /**
     * Called by the PDP after the usage is revoked or ended
     *
     * Remove the session from sessions or reset it to initial state when there are waiters available
     */
    override fun updateInformation(identifier: Any, newValue: Any?) {

        if (identifier !is String) {
            throw LuceException("Invalid identifier type in updateInformation: String expected")
        }

        if (LOG.isTraceEnabled) {
            LOG.trace("Remove usage session with id=$identifier")
        }

        // get session from sessions map
        val session = sessions[identifier] ?: throw LuceException("Session with id=$identifier unknown")

        // ensure we have the current lock, so we are currently responsible for the session
        if (!session.lock.isHeldByCurrentThread)
            throw LuceException("Session with id=$identifier not held by current thread")

        // cancel reevaluation timer
        session.cancelTimer()

        // remove the session from the map to avoid race conditions that could occur after we check for waiters
        sessions.remove(identifier)

        // check if there are other waiters, if so reset the session to initial config
        if (session.lock.hasQueuedThreads()) {
            if (LOG.isTraceEnabled) {
                LOG.trace("Session with id=$identifier has active waiters - only reset the session")
            }
            session.reset()
            sessions[identifier] = session
            session.unlock()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SessionPip::class.java)
    }

}
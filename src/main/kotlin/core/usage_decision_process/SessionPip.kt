package core.usage_decision_process

import core.control_flow_model.components.PolicyInformationPoint
import core.exceptions.LuceException
import java.util.concurrent.ConcurrentHashMap

/**
 * Policy Information Point, providing usage session information to the PDP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class SessionPip : PolicyInformationPoint {

    private val sessions = ConcurrentHashMap<String, UsageSession>()

    /**
     * Called by the PDP, returns a (locked) usage session
     */
    override fun queryInformation(informationId: String): UsageSession {

        // first check if the usage session already exists, otherwise create a new one
        val session = sessions.getOrPut(informationId) { UsageSession() }

        // lock session to get synchronized access
        session.lock()

        return session
    }

    /**
     * Called by the PDP after the usage is revoked or ended
     *
     * Remove the session from sessions or reset it to initial state when there are waiters available
     */
    override fun updateInformation(informationId: String, newValue: Any?) {

        // get session from sessions map
        val session = sessions[informationId] ?: throw LuceException("Session unknown")

        // ensure we have the current lock, so we are currently responsible for the session
        if (!session.lock.isHeldByCurrentThread) throw LuceException("Session not held by current thread")

        // remove the session from the map to avoid race conditions that could occur after we check for waiters
        sessions.remove(informationId)

        // check if there are other waiters, if so reset the session to initial config
        if (session.lock.hasQueuedThreads()) {
            session.reset()
            sessions[informationId] = session
            session.unlock()
        }
    }

}
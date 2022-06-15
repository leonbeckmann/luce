package core.usage_decision_process

import core.control_flow_model.components.ComponentRegistry
import core.exceptions.InUseException
import core.exceptions.LuceException
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Usage Session PIP, as mentioned in LUCE's control flow model (see Section 6.1.1)
 *
 * Provides usage session information to the PDP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
object SessionPip {

    private val LOG = LoggerFactory.getLogger(SessionPip::class.java)

    // all active sessions
    private val sessions = ConcurrentHashMap<String, UsageSession>()

    /**
     * Called by the PDP, returns a (locked) initial usage session
     */
    fun getLockedInitialSession(id: String) : UsageSession {

        if (LOG.isTraceEnabled) {
            LOG.trace("Query initial usage session with id=$id")
        }

        // expect initial session, which might already exist but must be reset
        val session = sessions.getOrPut(id) {
            if (LOG.isTraceEnabled) {
                LOG.trace("Put new usage session with id=$id")
            }
            UsageSession(id)
        }

        // lock session to get synchronized access
        session.lock()

        // check expected state
        if (session.state !is UsageSession.State.Initial) {
            val state = session.state
            session.unlock()

            // check if session is used for other access
            if (session.state is UsageSession.State.Accessing) {
                throw InUseException("Session with id=$id is currently used")
            }

            // not in initial state and not in accessing state, should not be in registry then
            throw LuceException("Session with id=$id in state=${state} is not in expected " +
                    "state=${UsageSession.State.Initial}")
        }

        return session
    }

    /**
     * Called by the PDP, returns a (locked) continuous usage session
     */
    fun getLockedContinuousSession(id: String) : UsageSession {

        if (LOG.isTraceEnabled) {
            LOG.trace("Query continuous usage session with id=$id")
        }

        // expect existent session
        val session = sessions[id] ?: throw LuceException("Expected session with id=$id does not exist")

        // lock session to get synchronized access
        session.lock()

        // check expected state
        if (session.state !is UsageSession.State.Accessing) {
            val state = session.state
            session.unlock()

            // not in accessing state
            throw LuceException("Session with id=$id in state=${state} is not in expected " +
                    "state=${UsageSession.State.Accessing}")
        }

        return session
    }

    /**
     * Called by the PDP to unlock the session again
     *
     * When the state is Error, End, Revoked or Denied, the session is removed or reset
     */
    fun finishLock(session: UsageSession) {

        // ensure we have the current lock, so we are currently responsible for the session
        if (!session.lock.isHeldByCurrentThread)
            throw LuceException("Session with id=${session.id} not held by current thread")

        if (session.state is UsageSession.State.Accessing) {
            // simply unlock session for further usage
            session.unlock()
            return
        }

        // otherwise, remove finished sessions from registry
        if (LOG.isTraceEnabled) {
            LOG.trace("Remove usage session with id=${session.id}")
        }

        // remove the session from the map to avoid race conditions that could occur after we check for waiters
        sessions.remove(session.id)
        // also remove the PIP that provides us with session information, e.g. PEP listener
        ComponentRegistry.removePolicyInformationPoint(session.id)

        // check if there are other waiters, if so reset the session to initial config
        if (session.lock.hasQueuedThreads()) {
            if (LOG.isTraceEnabled) {
                LOG.trace("Session with id=${session.id} has active waiters - only reset the session")
            }
            session.reset()
            // TODO check if there is a race condition when a session with the same ID is created while it is
            //  reinserted here -> if so lock the session registry
            sessions[session.id] = session
        }

        // unlock the session
        session.unlock()

    }
}
package core.usage_decision_process

import core.control_flow_model.components.PolicyEnforcementPoint
import core.control_flow_model.messages.RevocationResponse
import core.exceptions.InUseException
import core.exceptions.LuceException
import core.policies.LucePolicy
import it.unibo.tuprolog.core.Truth
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils
import java.util.concurrent.ConcurrentHashMap


internal class SessionPipTest {

    @Test
    fun sessionPipTest() {

        // create pip with empty sessions map
        @Suppress("UNCHECKED_CAST") val sessions: ConcurrentHashMap<String, UsageSession> =
            ReflectionTestUtils.getField(SessionPip,"sessions") as ConcurrentHashMap<String, UsageSession>
        assert(sessions.isEmpty())

        // query a new session
        var session = SessionPip.getLockedInitialSession("session1")
        assert(session.lock.isHeldByCurrentThread)
        assert(!session.lock.hasQueuedThreads())
        assert(sessions.size == 1)
        assert(session.state is UsageSession.Initial)
        session.feedEvent(UsageSession.TryAccess)
        assert(session.state is UsageSession.Requesting)
        session.feedEvent(UsageSession.PermitAccess(
            LucePolicy(Truth.TRUE, Truth.TRUE, Truth.TRUE, 5, Truth.TRUE, Truth.TRUE),
            object : PolicyEnforcementPoint {
                override fun onRevocation(response: RevocationResponse) {}
            },
            null
        ))
        assert(session.state is UsageSession.Accessing)
        SessionPip.finishLock(session)
        assert(sessions.size == 1)

        // cannot delete/reset unlocked sessions
        assertThrows<LuceException> {
            SessionPip.finishLock(session)
        }

        // cannot access new session1
        assertThrows<InUseException> {
            SessionPip.getLockedInitialSession("session1")
        }

        // lock again and remove
        session = SessionPip.getLockedContinuousSession("session1")
        assert(sessions.size == 1)
        assert(session.state is UsageSession.Accessing)
        session.feedEvent(UsageSession.EndAccess)
        assert(session.state is UsageSession.End)
        SessionPip.finishLock(session)
        assert(sessions.isEmpty())

        // register new session1
        val newSession = SessionPip.getLockedInitialSession("session1")
        assert(sessions.size == 1)
        assert(session != newSession)

        // run in second thread
        Thread {
            // get same session, this blocks until session is unlocked
            SessionPip.getLockedInitialSession("session1")
        }.start()

        // sleep until other thread waits for session
        Thread.sleep(200)

        // reset session
        SessionPip.finishLock(newSession)
        assert(sessions.size == 1) // not removed due to waiter
        assert(newSession == sessions.iterator().next().value)
        assert(newSession.state is UsageSession.Initial)
    }

}
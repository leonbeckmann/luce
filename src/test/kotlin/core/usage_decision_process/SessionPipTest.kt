package core.usage_decision_process

import core.exceptions.InUseException
import core.exceptions.LuceException
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
        var session = SessionPip.getLockedSession("session1", UsageSession.State.Initial)
        assert(session.lock.isHeldByCurrentThread)
        assert(!session.lock.hasQueuedThreads())
        assert(sessions.size == 1)
        assert(session.state == UsageSession.State.Initial)
        session.feedEvent(UsageSession.Event.TryAccess)
        assert(session.state == UsageSession.State.Requesting)
        session.feedEvent(UsageSession.Event.PermitAccess)
        assert(session.state == UsageSession.State.Accessing)
        SessionPip.finishLock(session)
        assert(sessions.size == 1)

        // cannot delete/reset unlocked sessions
        assertThrows<LuceException> {
            SessionPip.finishLock(session)
        }

        // cannot access new session1
        assertThrows<InUseException> {
            SessionPip.getLockedSession("session1", UsageSession.State.Initial)
        }

        // lock again and remove
        session = SessionPip.getLockedSession("session1", UsageSession.State.Accessing)
        assert(sessions.size == 1)
        assert(session.state == UsageSession.State.Accessing)
        session.feedEvent(UsageSession.Event.EndAccess)
        assert(session.state == UsageSession.State.End)
        SessionPip.finishLock(session)
        assert(sessions.isEmpty())

        // register new session1
        val newSession = SessionPip.getLockedSession("session1", UsageSession.State.Initial)
        assert(sessions.size == 1)
        assert(session != newSession)

        // run in second thread
        Thread {
            // get same session, this blocks until session is unlocked
            SessionPip.getLockedSession("session1", UsageSession.State.Initial)
        }.start()

        // sleep until other thread waits for session
        Thread.sleep(200)

        // reset session
        SessionPip.finishLock(newSession)
        assert(sessions.size == 1) // not removed due to waiter
        assert(newSession == sessions.iterator().next().value)
        assert(newSession.state == UsageSession.State.Initial)
    }

}
package core.usage_decision_process

import core.exceptions.LuceException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils
import java.util.concurrent.ConcurrentHashMap


internal class SessionPipTest {

    @Test
    fun sessionPipTest() {

        // create pip with empty sessions map
        val pip = SessionPip()
        @Suppress("UNCHECKED_CAST") val sessions: ConcurrentHashMap<String, UsageSession> =
            ReflectionTestUtils.getField(pip,"sessions") as ConcurrentHashMap<String, UsageSession>
        assert(sessions.isEmpty())

        // query a new session
        var session = pip.queryInformation("session1")
        assert(session.lock.isHeldByCurrentThread)
        assert(!session.lock.hasQueuedThreads())
        assert(sessions.size == 1)
        assert(session.state == UsageSession.State.Initial)
        session.feedEvent(UsageSession.Event.TryAccess)
        assert(session.state == UsageSession.State.Requesting)
        session.unlock()

        // cannot delete/reset unknown sessions
        assertThrows<LuceException> {
            pip.updateInformation("session2", null)
        }

        // cannot delete/reset unlocked sessions
        assertThrows<LuceException> {
            pip.updateInformation("session1", null)
        }

        // lock again and remove
        session = pip.queryInformation("session1")
        assert(sessions.size == 1)
        pip.updateInformation("session1", null)
        assert(sessions.isEmpty())
        session.unlock()

        // register new session1
        val newSession = pip.queryInformation("session1")
        assert(sessions.size == 1)
        assert(session != newSession)

        // run in second thread
        Thread {
            // get same session, this blocks until session is unlocked
            pip.queryInformation("session1")
        }.start()

        // sleep until other thread waits for session
        Thread.sleep(200)

        // reset session
        pip.updateInformation("session1", null)
        assert(sessions.size == 1) // not removed due to waiter
        assert(newSession == sessions.iterator().next().value)
        assert(newSession.state == UsageSession.State.Initial)
    }

}
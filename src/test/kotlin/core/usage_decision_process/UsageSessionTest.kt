package core.usage_decision_process

import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

internal class UsageSessionTest {

    private fun sessionTestHelper(
        s0: UsageSession.State,
        s1: UsageSession.State,
        event: UsageSession.Event
    ) {
        val session = UsageSession("id")
        ReflectionTestUtils.setField(session, "state", s0)
        try {
            session.lock()
            assert(session.state == s0)
            session.feedEvent(event)
            assert(session.state == s1)
        } finally {
            session.unlock()
        }
    }

    @Test
    fun usageSessionTest() {

        // check initial state
        val session = UsageSession("id")
        assert(session.state == UsageSession.State.Initial)

        // check transitions
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Requesting, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.PreUpdate)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.PreDeps)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.PermitAccess)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.OnUpdate)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.OnDeps)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.RevokeAccess)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.PostUpdate)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.PostDeps)

        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Requesting, UsageSession.Event.PreUpdate)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Requesting, UsageSession.Event.PreDeps)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Denied, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Accessing, UsageSession.Event.PermitAccess)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Error, UsageSession.Event.OnUpdate)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Error, UsageSession.Event.OnDeps)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Error, UsageSession.Event.RevokeAccess)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Error, UsageSession.Event.PostUpdate)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Error, UsageSession.Event.PostDeps)

        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Denied, UsageSession.Event.PreUpdate)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.PreDeps)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.PermitAccess)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.OnUpdate)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.OnDeps)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.RevokeAccess)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.PostUpdate)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.PostDeps)

        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.Error, UsageSession.Event.PreUpdate)
        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.Error, UsageSession.Event.PreDeps)
        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.Error, UsageSession.Event.PermitAccess)
        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.Accessing, UsageSession.Event.OnUpdate)
        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.Accessing, UsageSession.Event.OnDeps)
        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.End, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.Revoked, UsageSession.Event.RevokeAccess)
        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.Error, UsageSession.Event.PostUpdate)
        sessionTestHelper(UsageSession.State.Accessing, UsageSession.State.Error, UsageSession.Event.PostDeps)

        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.PreUpdate)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.PreDeps)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.PermitAccess)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.OnUpdate)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.OnDeps)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.RevokeAccess)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.End, UsageSession.Event.PostUpdate)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.End, UsageSession.Event.PostDeps)

        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.PreUpdate)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.PreDeps)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.PermitAccess)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.OnUpdate)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.OnDeps)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.RevokeAccess)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Revoked, UsageSession.Event.PostUpdate)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Revoked, UsageSession.Event.PostDeps)

        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.PreUpdate)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.PreDeps)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.PermitAccess)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.OnUpdate)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.OnDeps)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.RevokeAccess)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.PostUpdate)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.PostDeps)
    }
}
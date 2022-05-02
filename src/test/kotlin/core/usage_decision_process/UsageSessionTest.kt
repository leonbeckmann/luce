package core.usage_decision_process

import core.control_flow_model.components.PolicyEnforcementPoint
import core.control_flow_model.messages.RevocationResponse
import core.policies.LucePolicy
import it.unibo.tuprolog.core.Truth
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

        val policy = LucePolicy(Truth.TRUE, Truth.TRUE, Truth.TRUE, 5, Truth.TRUE, Truth.TRUE)
        val pep = object : PolicyEnforcementPoint {
            override fun onRevocation(response: RevocationResponse) {}
        }

        // check transitions
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Requesting, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error,
            UsageSession.Event.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Initial, UsageSession.State.Error, UsageSession.Event.RevokeAccess)

        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Denied, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Accessing(policy, pep, null),
            UsageSession.Event.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Requesting, UsageSession.State.Error, UsageSession.Event.RevokeAccess)

        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error,
            UsageSession.Event.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Denied, UsageSession.State.Error, UsageSession.Event.RevokeAccess)

        sessionTestHelper(UsageSession.State.Accessing(policy, pep, null), UsageSession.State.Error,
            UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Accessing(policy, pep, null), UsageSession.State.Error,
            UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Accessing(policy, pep, null), UsageSession.State.Error,
            UsageSession.Event.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.State.Accessing(policy, pep, null), UsageSession.State.End,
            UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Accessing(policy, pep, null), UsageSession.State.Revoked,
            UsageSession.Event.RevokeAccess)

        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error,
            UsageSession.Event.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.End, UsageSession.State.Error, UsageSession.Event.RevokeAccess)

        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error,
            UsageSession.Event.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Revoked, UsageSession.State.Error, UsageSession.Event.RevokeAccess)

        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.TryAccess)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.DenyAccess)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error,
            UsageSession.Event.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.EndAccess)
        sessionTestHelper(UsageSession.State.Error, UsageSession.State.Error, UsageSession.Event.RevokeAccess)
    }
}
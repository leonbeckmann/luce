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
        assert(session.state == UsageSession.Initial)

        val policy = LucePolicy(Truth.TRUE, Truth.TRUE, Truth.TRUE, 5, Truth.TRUE, Truth.TRUE)
        val pep = object : PolicyEnforcementPoint {
            override fun onRevocation(response: RevocationResponse) {}
        }

        // check transitions
        sessionTestHelper(UsageSession.Initial, UsageSession.Requesting, UsageSession.TryAccess)
        sessionTestHelper(UsageSession.Initial, UsageSession.Error, UsageSession.DenyAccess)
        sessionTestHelper(UsageSession.Initial, UsageSession.Error,
            UsageSession.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.Initial, UsageSession.Error, UsageSession.EndAccess)
        sessionTestHelper(UsageSession.Initial, UsageSession.Error, UsageSession.RevokeAccess)

        sessionTestHelper(UsageSession.Requesting, UsageSession.Error, UsageSession.TryAccess)
        sessionTestHelper(UsageSession.Requesting, UsageSession.Denied, UsageSession.DenyAccess)
        sessionTestHelper(UsageSession.Requesting, UsageSession.Accessing(policy, pep, null),
            UsageSession.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.Requesting, UsageSession.Error, UsageSession.EndAccess)
        sessionTestHelper(UsageSession.Requesting, UsageSession.Error, UsageSession.RevokeAccess)

        sessionTestHelper(UsageSession.Denied, UsageSession.Error, UsageSession.TryAccess)
        sessionTestHelper(UsageSession.Denied, UsageSession.Error, UsageSession.DenyAccess)
        sessionTestHelper(UsageSession.Denied, UsageSession.Error,
            UsageSession.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.Denied, UsageSession.Error, UsageSession.EndAccess)
        sessionTestHelper(UsageSession.Denied, UsageSession.Error, UsageSession.RevokeAccess)

        sessionTestHelper(UsageSession.Accessing(policy, pep, null), UsageSession.Error,
            UsageSession.TryAccess)
        sessionTestHelper(UsageSession.Accessing(policy, pep, null), UsageSession.Error,
            UsageSession.DenyAccess)
        sessionTestHelper(UsageSession.Accessing(policy, pep, null), UsageSession.Error,
            UsageSession.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.Accessing(policy, pep, null), UsageSession.End,
            UsageSession.EndAccess)
        sessionTestHelper(UsageSession.Accessing(policy, pep, null), UsageSession.Revoked,
            UsageSession.RevokeAccess)

        sessionTestHelper(UsageSession.End, UsageSession.Error, UsageSession.TryAccess)
        sessionTestHelper(UsageSession.End, UsageSession.Error, UsageSession.DenyAccess)
        sessionTestHelper(UsageSession.End, UsageSession.Error,
            UsageSession.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.End, UsageSession.Error, UsageSession.EndAccess)
        sessionTestHelper(UsageSession.End, UsageSession.Error, UsageSession.RevokeAccess)

        sessionTestHelper(UsageSession.Revoked, UsageSession.Error, UsageSession.TryAccess)
        sessionTestHelper(UsageSession.Revoked, UsageSession.Error, UsageSession.DenyAccess)
        sessionTestHelper(UsageSession.Revoked, UsageSession.Error,
            UsageSession.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.Revoked, UsageSession.Error, UsageSession.EndAccess)
        sessionTestHelper(UsageSession.Revoked, UsageSession.Error, UsageSession.RevokeAccess)

        sessionTestHelper(UsageSession.Error, UsageSession.Error, UsageSession.TryAccess)
        sessionTestHelper(UsageSession.Error, UsageSession.Error, UsageSession.DenyAccess)
        sessionTestHelper(UsageSession.Error, UsageSession.Error,
            UsageSession.PermitAccess(policy, pep, null))
        sessionTestHelper(UsageSession.Error, UsageSession.Error, UsageSession.EndAccess)
        sessionTestHelper(UsageSession.Error, UsageSession.Error, UsageSession.RevokeAccess)
    }
}
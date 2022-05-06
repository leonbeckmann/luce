package core.policies

import core.usage_control_requirements.UsageControlTests
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.dsl.prolog
import it.unibo.tuprolog.core.List as PrologList
import org.junit.jupiter.api.Test

internal class LucePolicyTest {

    @Test
    fun testLucePolicyReplacement() {

        val policy = LucePolicy(
            preAccess = prolog {
                "resolve_string_list"("test_pip_attr:\$SUBJECT.organizations", "X") and
                "resolve_string_list"("test_pip_attr:\$OBJECT.authorized", "Y") and
                "intersection"("X", "Y", "Z") and
                "not"("list_empty"("Z"))
            },
            postPermit = PrologList.of(Atom.of("a"), Atom.of("\$OBJECT"), Atom.of("c")),
            ongoingAccess = prolog {
                "now"("test_pip_time", "A") and
                "resolve_string"("test_pip_attr:\$SUBJECT.identity", "B") and
                "resolve_string"("test_pip_attr:\$OBJECT.identity", "C") and
                "usage_notification"("A", "B", "C", "\$RIGHT", "D") and
                "notify_monitor"("D", UsageControlTests.DefaultNotification.id())
            },
            ongoingPeriod = null,
            postAccessRevoked =  prolog {
                "decrement"("test_pip_attr:\$OBJECT.semaphore")
            },
            postAccessEnded = prolog { ">"(5, 7) }
        )

        val replacedPolicy = policy.replaceVariables("s1", "o1", "r1")

        assert(replacedPolicy.preAccess.toString().contains("test_pip_attr:s1.organizations"))
        assert(replacedPolicy.preAccess.toString().contains("test_pip_attr:o1.authorized"))
        assert(!replacedPolicy.preAccess.toString().contains("\$OBJECT"))
        assert(!replacedPolicy.preAccess.toString().contains("\$SUBJECT"))

        assert(replacedPolicy.postPermit.toString() == "[a, o1, c]")

        assert(replacedPolicy.ongoingAccess.toString().contains("test_pip_attr:s1.identity"))
        assert(replacedPolicy.ongoingAccess.toString().contains("test_pip_attr:o1.identity"))
        assert(replacedPolicy.ongoingAccess.toString().contains("r1, D"))
        assert(!replacedPolicy.ongoingAccess.toString().contains("\$SUBJECT"))
        assert(!replacedPolicy.ongoingAccess.toString().contains("\$OBJECT"))
        assert(!replacedPolicy.ongoingAccess.toString().contains("\$RIGHT"))

        assert(replacedPolicy.postAccessRevoked.toString() == "decrement('test_pip_attr:o1.semaphore')")

        assert(replacedPolicy.postAccessEnded == policy.postAccessEnded)
    }

}
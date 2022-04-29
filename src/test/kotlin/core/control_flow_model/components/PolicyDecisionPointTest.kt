package core.control_flow_model.components

import core.admin.LuceObject
import core.admin.LuceRight
import core.admin.LuceSubject
import core.control_flow_model.messages.DecisionRequest
import core.logic.PolicyEvaluator
import core.policies.LucePolicy
import core.usage_decision_process.SessionPip
import core.usage_decision_process.UsageSession
import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.dsl.prolog
import it.unibo.tuprolog.solve.library.Library
import it.unibo.tuprolog.theory.parsing.ClausesParser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.util.concurrent.ConcurrentHashMap

internal class PolicyDecisionPointTest {

    class ExampleObject(
        identity: String,
        owner: String
    ) : LuceObject<String, String>(identity, owner, "example", setOf())

    class ExampleSubject(
        identity: String,
        owner: String
    ) : LuceSubject<String>(identity, owner, setOf())

    class ExamplePmpPreFailure : PolicyManagementPoint {
        override fun pullPolicy(): LucePolicy {
            return LucePolicy(
                preAccess = Truth.FALSE,
                postPermit = Truth.TRUE,
                ongoingAccess = Truth.TRUE,
                ongoingPeriod = 500,
                postAccessEnded = Truth.TRUE,
                postAccessRevoked = Truth.TRUE
            )
        }
    }

    class ExamplePmpOngoingSuccess : PolicyManagementPoint {
        override fun pullPolicy(): LucePolicy {
            return LucePolicy(
                preAccess = prolog { "father"("abraham", "Y") } ,
                postPermit = Truth.TRUE,
                ongoingAccess = Truth.TRUE,
                ongoingPeriod = 500,
                postAccessEnded = Truth.TRUE,
                postAccessRevoked = Truth.TRUE
            )
        }
    }

    class ExamplePmpOngoingFailure : PolicyManagementPoint {
        override fun pullPolicy(): LucePolicy {
            return LucePolicy(
                preAccess = prolog { "father"("abraham", "Y") } ,
                postPermit = Truth.TRUE,
                ongoingAccess = Truth.FALSE,
                ongoingPeriod = 500,
                postAccessEnded = Truth.TRUE,
                postAccessRevoked = Truth.TRUE
            )
        }
    }

    companion object {

        private lateinit var sessions: ConcurrentHashMap<String, UsageSession>

        @BeforeAll
        @JvmStatic
        fun initPdpTest() {

            // set logging level for pdpTest to TRACE
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")

            // load custom prolog library
            PolicyEvaluator.registerCustomLibrary(
                Library.aliased(
                    alias = "custom_library",
                    // operatorSet = ,
                    theory = ClausesParser.withDefaultOperators.parseTheory(
                        """
                        grandfather(X, Y) :- father(X, Z), father(Z, Y).
                        father(abraham, isaac).
                        father(isaac, jacob).
                    """.trimIndent()
                    ),
                    // primitives = ,
                    // functions = ,
                )
            )

            // access sessions for test
            @Suppress("UNCHECKED_CAST")
            sessions = ReflectionTestUtils.getField(SessionPip,"sessions") as ConcurrentHashMap<String, UsageSession>
        }
    }

    @AfterEach
    fun cleanup() {

        // clear timer
        sessions.forEach {
            it.value.reevaluationTimer?.cancel()
        }

        // clear sessions
        sessions.clear()

        // for better logging output
        println()
    }

    @Test
    fun pdpTestPreFailure() {

        // register PMP
        ComponentRegistry.policyManagementPoint = ExamplePmpPreFailure()

        assert(sessions.isEmpty())

        // make decision
        val request = DecisionRequest(
            ExampleSubject("object1", "subject1"),
            ExampleObject("subject1", "root"),
            LuceRight("example")
        )
        val decision = PolicyDecisionPoint.requestDecision(request)
        assert(decision.isDenied())
        assert(sessions.isEmpty())

    }

    @Test
    fun pdpTestOngoingFailure() {

        // register PMP
        ComponentRegistry.policyManagementPoint = ExamplePmpOngoingFailure()

        assert(sessions.isEmpty())

        // make decision
        val request = DecisionRequest(
            ExampleSubject("object1", "subject1"),
            ExampleObject("subject1", "root"),
            LuceRight("example")
        )
        val decision = PolicyDecisionPoint.requestDecision(request)
        assert(decision.isPermitted())
        assert(sessions.size == 1)

        val session = sessions.iterator().next().value
        assert(session.state == UsageSession.State.Accessing)

        // sleep until re-evaluation is over
        Thread.sleep(800)

        // ensure that session has been revoked
        assert(sessions.isEmpty())
        assert(session.state == UsageSession.State.Revoked)
        assert(session.reevaluationTimer == null)

    }

    @Test
    fun pdpTestOngoingSuccess() {

        // register PMP
        ComponentRegistry.policyManagementPoint = ExamplePmpOngoingSuccess()

        assert(sessions.isEmpty())

        // make decision
        val request = DecisionRequest(
            ExampleSubject("object1", "subject1"),
            ExampleObject("subject1", "root"),
            LuceRight("example")
        )
        val decision = PolicyDecisionPoint.requestDecision(request)
        assert(decision.isPermitted())
        assert(sessions.size == 1)

        val session = sessions.iterator().next().value
        assert(session.state == UsageSession.State.Accessing)

        // sleep until re-evaluation is over
        Thread.sleep(800)

        // ensure that session is still available
        assert(sessions.size == 1)
        assert(session.state == UsageSession.State.Accessing)
        assert(sessions.iterator().next().value == session)

    }

}
package core.control_flow_model.components

import core.admin.LuceObject
import core.admin.LuceRight
import core.admin.LuceSubject
import core.control_flow_model.messages.DecisionRequest
import core.control_flow_model.messages.EndRequest
import core.control_flow_model.messages.RevocationResponse
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
import java.util.concurrent.CountDownLatch

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
        override fun <Sid, Oid> pullPolicy(obj: LuceObject<Sid, Oid>, right: LuceRight): List<LucePolicy> {
            return listOf(LucePolicy(
                preAccess = Truth.FALSE,
                postPermit = Truth.TRUE,
                ongoingAccess = Truth.TRUE,
                ongoingPeriod = 500,
                postAccessEnded = Truth.TRUE,
                postAccessRevoked = Truth.TRUE
            ))
        }
    }

    class ExamplePmpOngoingSuccess : PolicyManagementPoint {
        override fun <Sid, Oid> pullPolicy(obj: LuceObject<Sid, Oid>, right: LuceRight): List<LucePolicy> {
            return listOf(LucePolicy(
                preAccess = prolog { "father"("abraham", "Y") } ,
                postPermit = Truth.TRUE,
                ongoingAccess = Truth.TRUE,
                ongoingPeriod = 500,
                postAccessEnded = Truth.TRUE,
                postAccessRevoked = Truth.TRUE
            ))
        }
    }

    class ExamplePmpOngoingFailure : PolicyManagementPoint {
        override fun <Sid, Oid> pullPolicy(obj: LuceObject<Sid, Oid>, right: LuceRight): List<LucePolicy> {
            return listOf(LucePolicy(
                preAccess = prolog { "father"("abraham", "Y") } ,
                postPermit = Truth.TRUE,
                ongoingAccess = Truth.FALSE,
                ongoingPeriod = 500,
                postAccessEnded = Truth.TRUE,
                postAccessRevoked = Truth.TRUE
            ))
        }
    }

    class ExamplePep(private val latch : CountDownLatch) : PolicyEnforcementPoint {
        override fun onRevocation(response: RevocationResponse) {
            latch.countDown()
        }

        override fun doDependency(dependencyId: String): Boolean = true
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
            it.value.cancelTimer()
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

        val o = ExampleObject("object1", "subject1")
        val s = ExampleSubject("subject1", "root")
        val r = LuceRight("example")

        // make decision
        val request = DecisionRequest(s, o, r, ExamplePep(CountDownLatch(0)))
        val decision = PolicyDecisionPoint.requestDecision(request)
        assert(decision.isDenied())
        assert(sessions.isEmpty())
    }

    @Test
    fun pdpTestOngoingFailure() {

        // register PMP
        ComponentRegistry.policyManagementPoint = ExamplePmpOngoingFailure()

        assert(sessions.isEmpty())

        val o = ExampleObject("object1", "subject1")
        val s = ExampleSubject("subject1", "root")
        val r = LuceRight("example")

        // make decision
        val latch = CountDownLatch(1)
        val request = DecisionRequest(s, o, r, ExamplePep(latch))
        val decision = PolicyDecisionPoint.requestDecision(request)
        assert(decision.isPermitted())
        assert(sessions.size == 1)

        val session = sessions.iterator().next().value
        assert(session.state is UsageSession.State.Accessing)

        // sleep until re-evaluation is over and onRevocation was called on the PEP
        latch.await()

        // ensure that session has been revoked
        assert(sessions.isEmpty())
        assert(session.state is UsageSession.State.Revoked)
    }

    @Test
    fun pdpTestOngoingSuccess() {

        // register PMP
        ComponentRegistry.policyManagementPoint = ExamplePmpOngoingSuccess()

        sessions.clear()
        assert(sessions.isEmpty())

        val o = ExampleObject("object1", "subject1")
        val s = ExampleSubject("subject1", "root")
        val r = LuceRight("example")

        // make decision
        val latch = CountDownLatch(1)
        val request = DecisionRequest(s, o, r, ExamplePep(latch))

        val decision = PolicyDecisionPoint.requestDecision(request)
        assert(decision.isPermitted())
        assert(sessions.size == 1)

        val session = sessions.iterator().next().value
        assert(session.state is UsageSession.State.Accessing)

        // sleep until re-evaluation is over
        Thread.sleep(800)

        // ensure that session is still available
        assert(sessions.size == 1)
        assert(session.state is UsageSession.State.Accessing)
        assert(sessions.iterator().next().value == session)
        assert(latch.count == 1L)

        // end access
        val endRequest = EndRequest(s, o, r)
        PolicyDecisionPoint.endUsage(endRequest)
    }
}
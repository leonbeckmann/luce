package core.control_flow_model.components

import core.admin.LuceObject
import core.admin.LuceRight
import core.admin.LuceSubject
import core.control_flow_model.messages.DecisionRequest
import core.logic.PolicyEvaluator
import core.policies.LucePolicy
import core.usage_decision_process.SessionPip
import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.dsl.prolog
import it.unibo.tuprolog.solve.library.Library
import it.unibo.tuprolog.theory.parsing.ClausesParser
import org.junit.jupiter.api.Test

internal class PolicyDecisionPointTest {

    class ExampleObject(
        identity: String,
        owner: String
    ) : LuceObject<String, String>(identity, owner, "example", setOf())

    class ExampleSubject(
        identity: String,
        owner: String
    ) : LuceSubject<String>(identity, owner, setOf())

    class ExamplePmp : PolicyManagementPoint {
        override fun pullPolicy(): LucePolicy {
            return LucePolicy(
                preAccess = prolog { "father"("abraham", "Y") } ,
                Truth.TRUE,
                Truth.TRUE,
                Truth.TRUE,
                Truth.TRUE
            )
        }
    }

    @Test
    fun pdpTest() {

        // set logging level for pdpTest to TRACE
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");

        // register PMP
        ComponentRegistry.policyManagementPoint = ExamplePmp()

        // register PIPs
        ComponentRegistry.addPolicyInformationPoint("usage_session", SessionPip())

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

        val request = DecisionRequest(
            ExampleSubject("object1", "subject1"),
            ExampleObject("subject1", "root"),
            LuceRight("example")
        )

        PolicyDecisionPoint.requestDecision(request)

    }

}
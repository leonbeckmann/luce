package core.control_flow_model.components

import core.admin.LuceObject
import core.admin.LuceRight
import core.admin.LuceSubject
import core.control_flow_model.messages.DecisionRequest
import core.logic.PolicyEvaluator
import core.policies.LucePolicy
import core.usage_decision_process.SessionPip
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.core.Clause
import it.unibo.tuprolog.core.Truth
import it.unibo.tuprolog.solve.library.Library
import it.unibo.tuprolog.theory.Theory
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
            return LucePolicy()
        }
    }

    @Test
    fun pdpTest() {

        // register PMP
        ComponentRegistry.policyManagementPoint = ExamplePmp()

        // register PIPs
        ComponentRegistry.addPolicyInformationPoint("usage_session", SessionPip())

        // load custom prolog library
        PolicyEvaluator.loadCustomLibrary(
            Library.aliased(
                alias = "custom_library",
                // operatorSet = ,
                theory = Theory.indexedOf(
                    Clause.of(Atom.of("Alice"), Truth.TRUE),
                    Clause.of(Atom.of("Bob"), Truth.TRUE),
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
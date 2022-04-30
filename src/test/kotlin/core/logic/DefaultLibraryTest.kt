package core.logic

import core.control_flow_model.components.ComponentRegistry
import core.control_flow_model.components.PolicyInformationPoint
import it.unibo.tuprolog.dsl.prolog
import it.unibo.tuprolog.solve.SolveOptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class DefaultLibraryTest {

    class IntegerPip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): Int {
            return 3
        }

        override fun updateInformation(identifier: Any, newValue: Any?) {}
    }

    class RealPip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): Double {
            return 1.5
        }

        override fun updateInformation(identifier: Any, newValue: Any?) {}
    }

    class TruthPip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): Boolean {
            return true
        }

        override fun updateInformation(identifier: Any, newValue: Any?) {}
    }

    class StringPip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): String {
            return "a"
        }

        override fun updateInformation(identifier: Any, newValue: Any?) {}
    }

    class StringListPip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): List<String> {
            return listOf("a", "b", "c")
        }

        override fun updateInformation(identifier: Any, newValue: Any?) {}
    }

    companion object {

        @BeforeAll
        @JvmStatic
        fun registerPips() {
            ComponentRegistry.addPolicyInformationPoint("test_pip_int", IntegerPip())
            ComponentRegistry.addPolicyInformationPoint("test_pip_real", RealPip())
            ComponentRegistry.addPolicyInformationPoint("test_pip_truth", TruthPip())
            ComponentRegistry.addPolicyInformationPoint("test_pip_string", StringPip())
            ComponentRegistry.addPolicyInformationPoint("test_pip_string_list", StringListPip())
        }

    }

    @Test
    fun testResolveInt() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "resolveInt"("test_pip_int:attr1", "X") and "="("X", 3) },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testResolveReal() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "resolveReal"("test_pip_real:attr1", "X") and "="("X", 1.5) },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testResolveTruth() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "resolveTruth"("test_pip_truth:attr1", "X") and "="("X", true) },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testResolveString() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "resolveString"("test_pip_string:attr1", "X") and "="("X", "a")},
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testResolveStringList() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "resolveStringList"("test_pip_string_list:attr1", "X") and "member"("c", "X")},
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }
}
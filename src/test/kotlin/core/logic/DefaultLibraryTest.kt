package core.logic

import core.control_flow_model.components.ComponentRegistry
import core.control_flow_model.components.PolicyInformationPoint
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.dsl.prolog
import it.unibo.tuprolog.core.List as PrologList
import it.unibo.tuprolog.solve.SolveOptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class DefaultLibraryTest {

    class IntegerPip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): Int {
            return 3
        }

        override fun updateInformationByValue(identifier: Any, newValue: Any?) : Boolean = true
        override fun updateInformation(identifier: Any, description: String): Boolean = true
    }

    class RealPip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): Double {
            return 1.5
        }

        override fun updateInformationByValue(identifier: Any, newValue: Any?) : Boolean = true
        override fun updateInformation(identifier: Any, description: String): Boolean = true
    }

    class TruthPip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): Boolean {
            return true
        }

        override fun updateInformationByValue(identifier: Any, newValue: Any?) : Boolean = true
        override fun updateInformation(identifier: Any, description: String): Boolean = true
    }

    class StringPip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): String {
            return "a"
        }

        override fun updateInformationByValue(identifier: Any, newValue: Any?) : Boolean = true
        override fun updateInformation(identifier: Any, description: String): Boolean = true
    }

    class StringListPip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): List<String> {
            return listOf("a", "b", "c")
        }

        override fun updateInformationByValue(identifier: Any, newValue: Any?) : Boolean = true
        override fun updateInformation(identifier: Any, description: String): Boolean = true
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
    fun testEmptyList() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "list_empty"(PrologList.empty()) and "not"("list_empty"(PrologList.of(Atom.of("a")))) },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testListIntersection() {
        val solution = PolicyEvaluator.evaluate(
            prolog {
                "intersection"(PrologList.empty(), PrologList.of(Atom.of("a")), PrologList.empty()) and
                "intersection"(PrologList.of(Atom.of("a"), Atom.of("b")), PrologList.of(Atom.of("c"),
                    Atom.of("d")), PrologList.empty()) and "intersection"(PrologList.of(Atom.of("a"),
                    Atom.of("b")), PrologList.of(Atom.of("b"), Atom.of("c")), PrologList.of(Atom.of("b")))
            },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testResolveInt() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "resolve_int"("test_pip_int:attr1", "X") and "="("X", 3) },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testResolveReal() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "resolve_real"("test_pip_real:attr1", "X") and "="("X", 1.5) },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testResolveTruth() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "resolve_truth"("test_pip_truth:attr1", "X") and "="("X", true) },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testResolveString() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "resolve_string"("test_pip_string:attr1", "X") and "="("X", "a")},
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testResolveStringList() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "resolve_string_list"("test_pip_string_list:attr1", "X") and "member"("c", "X")},
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    /*@Test
    fun testPipIdentifier() {
        val solution = PolicyEvaluator.evaluate(
            prolog { "pip_identifier"("a", "b", "a:b") },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }*/
}
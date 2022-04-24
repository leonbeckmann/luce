package core.logic

import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.solve.Solution
import it.unibo.tuprolog.solve.SolveOptions
import it.unibo.tuprolog.solve.Solver
import it.unibo.tuprolog.solve.library.AliasedLibrary
import it.unibo.tuprolog.solve.library.Library

/**
 * Policy Evaluator, used for solving PROLOG queries
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class PolicyEvaluator {

    companion object {

        private val libraries = mutableSetOf<AliasedLibrary>(DefaultLibrary())

        fun registerCustomLibrary(library: AliasedLibrary) {
            libraries.add(library)
        }

        fun unregisterCustomLibrary(library: AliasedLibrary) {
            libraries.remove(library)
        }

        fun evaluate(goal: Struct, options: SolveOptions): Solution {

            // configure solver
            val solver = Solver.prolog.mutableSolverWithDefaultBuiltins()
            libraries.iterator().forEach {
                solver.loadLibrary(it)
            }

            // solve and return solution
            return solver.solveOnce(goal, options)
        }

    }
}
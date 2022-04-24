package core.logic

import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.solve.Solution
import it.unibo.tuprolog.solve.SolveOptions
import it.unibo.tuprolog.solve.Solver
import it.unibo.tuprolog.solve.library.AliasedLibrary

/**
 * Policy Evaluator, used for solving PROLOG queries
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class PolicyEvaluator {

    companion object {

        private val solver = Solver.prolog.mutableSolverWithDefaultBuiltins()
        private val defaultLibrary = DefaultLibrary()

        init {
            // configure solver
            solver.loadLibrary(defaultLibrary)
        }

        // TODO load theories, clauses

        fun loadCustomLibrary(library: AliasedLibrary) {
            solver.loadLibrary(library)
        }

        fun unloadCustomLibrary(library: AliasedLibrary) {
            // ensure to not unload default library
            if (library != defaultLibrary) {
                solver.unloadLibrary(library)
            }
        }

        fun evaluate(goal: Struct, options: SolveOptions): Solution {
            return solver.clone().solveOnce(goal, options)
        }

    }
}
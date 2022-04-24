package core.logic

import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.solve.Solution
import it.unibo.tuprolog.solve.SolveOptions
import it.unibo.tuprolog.solve.Solver
import it.unibo.tuprolog.solve.library.AliasedLibrary
import org.slf4j.LoggerFactory

/**
 * Policy Evaluator, used for solving PROLOG queries
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class PolicyEvaluator {

    companion object {

        private val LOG = LoggerFactory.getLogger(PolicyEvaluator::class.java)
        private val libraries = mutableSetOf<AliasedLibrary>(DefaultLibrary())

        fun registerCustomLibrary(library: AliasedLibrary) {
            if (LOG.isTraceEnabled) {
                LOG.trace("Register custom library with alias=${library.alias}")
            }
            libraries.add(library)
        }

        fun unregisterCustomLibrary(library: AliasedLibrary) {
            if (LOG.isTraceEnabled) {
                LOG.trace("Unregister custom library with alias=${library.alias}")
            }
            libraries.remove(library)
        }

        fun evaluate(goal: Struct, options: SolveOptions): Solution {

            if (LOG.isTraceEnabled) {
                LOG.trace("Evaluate policy goal=$goal")
            }

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
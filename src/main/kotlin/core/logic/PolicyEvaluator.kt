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

        // a library registry to inject own PROLOG code into the usage decision
        private val libraries = mutableSetOf<AliasedLibrary>(DefaultLibrary())

        /**
         * Register a custom library with own PROLOG code
         */
        fun registerCustomLibrary(library: AliasedLibrary) {
            if (LOG.isTraceEnabled) {
                LOG.trace("Register custom library with alias=${library.alias}")
            }
            libraries.add(library)
        }

        /**
         * Unregister a custom library
         */
        fun unregisterCustomLibrary(library: AliasedLibrary) {
            if (LOG.isTraceEnabled) {
                LOG.trace("Unregister custom library with alias=${library.alias}")
            }
            libraries.remove(library)
        }

        /**
         * Evaluate a policy decision by querying the goal statement
         *
         * @return the solution, containing the decision result and applied substitutions
         */
        fun evaluate(goal: Struct, options: SolveOptions): Solution {

            if (LOG.isTraceEnabled) {
                LOG.trace("Evaluate policy goal=$goal")
            }

            // configure solver
            val solver = Solver.prolog.mutableSolverWithDefaultBuiltins()
            libraries.iterator().forEach {
                solver.loadLibrary(it)
            }

            // solve one possible solution and return
            return solver.solveOnce(goal, options)
        }

    }
}
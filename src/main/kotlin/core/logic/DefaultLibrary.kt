package core.logic

import it.unibo.tuprolog.core.operators.OperatorSet
import it.unibo.tuprolog.solve.Signature
import it.unibo.tuprolog.solve.function.LogicFunction
import it.unibo.tuprolog.solve.library.AliasedLibrary
import it.unibo.tuprolog.solve.primitive.Primitive
import it.unibo.tuprolog.theory.Theory

/**
 * Default 2Prolog Library, implements basic PROLOG functionalities used by the PolicyEvaluator
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class DefaultLibrary : AliasedLibrary {

    override val alias: String = "default_library"

    override val functions: Map<Signature, LogicFunction> = mapOf()

    override val operators: OperatorSet = OperatorSet.DEFAULT

    override val primitives: Map<Signature, Primitive> = mapOf()

    override val theory: Theory = Theory.empty()

}
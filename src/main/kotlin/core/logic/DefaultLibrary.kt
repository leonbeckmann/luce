package core.logic

import core.control_flow_model.components.ComponentRegistry
import core.exceptions.LuceException
import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.core.List as PrologList
import it.unibo.tuprolog.core.operators.OperatorSet
import it.unibo.tuprolog.solve.ExecutionContext
import it.unibo.tuprolog.solve.Signature
import it.unibo.tuprolog.solve.exception.error.SyntaxError
import it.unibo.tuprolog.solve.exception.error.SystemError
import it.unibo.tuprolog.solve.function.LogicFunction
import it.unibo.tuprolog.solve.library.AliasedLibrary
import it.unibo.tuprolog.solve.primitive.BinaryRelation
import it.unibo.tuprolog.solve.primitive.Primitive
import it.unibo.tuprolog.solve.primitive.Solve
import it.unibo.tuprolog.theory.Theory
import it.unibo.tuprolog.unify.Unificator.Companion.mguWith

/**
 * Default 2Prolog Library, implements basic PROLOG functionalities used by the PolicyEvaluator
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class DefaultLibrary : AliasedLibrary {

    override val alias: String = "default_library"

    override val functions: Map<Signature, LogicFunction> = mapOf()

    override val operators: OperatorSet = OperatorSet.DEFAULT

    override val primitives: Map<Signature, Primitive> = mapOf(
        ResolveInt.descriptionPair,
        ResolveReal.descriptionPair,
        ResolveTruth.descriptionPair,
        ResolveString.descriptionPair,
        ResolveStringList.descriptionPair
    )

    override val theory: Theory = Theory.empty()

    companion object {

        private fun resolveHelper(value: String, context: ExecutionContext) : Any? {

            val tokens = value.split(":")
            if (tokens.size != 2) {
                throw SyntaxError.of(
                    context,
                    "String of form <pip_identifier>:<attribute_identifier> expected, got $value"
                )
            }

            val pip = ComponentRegistry.policyInformationPoints[tokens[0]]
                ?: throw SystemError.forUncaughtException(
                    context,
                    LuceException("PIP with id=${tokens[0]} is not registered")
                )

            return pip.queryInformation(tokens[1])
        }

    }

    /**
     * resolveInt/2
     *
     * A prolog predicate that resolves pip_identifier:attribute_identifier to the actual attribute value of type Integer
     *
     * Expected format: resolveInt(Atom(<pip_id>:<attr_id>), Var)
     *
     * @return: the MGU of attr_val and Var iff attr_val is Int or Long
     */
    object ResolveInt : BinaryRelation.Functional<ExecutionContext>("resolveInt") {
        override fun Solve.Request<ExecutionContext>.computeOneSubstitution(first: Term, second: Term): Substitution {

            ensuringArgumentIsAtom(0)
            ensuringArgumentIsVariable(1)

            return when (val attrVal = resolveHelper(first.castToAtom().value, context)) {
                is Int -> Integer.of(attrVal).mguWith(second)
                is Long -> Integer.of(attrVal).mguWith(second)
                else -> {
                    throw SystemError.forUncaughtException(
                        context,
                        LuceException("Attribute is not an Integer")
                    )
                }
            }
        }
    }

    /**
     * resolveReal/2
     *
     * A prolog predicate that resolves pip_identifier:attribute_identifier to the actual attribute value of type Real
     *
     * Expected format: resolveReal(Atom(<pip_id>:<attr_id>), Var)
     *
     * @return: the MGU of attr_val and Var iff attr_val is Real
     */
    object ResolveReal : BinaryRelation.Functional<ExecutionContext>("resolveReal") {
        override fun Solve.Request<ExecutionContext>.computeOneSubstitution(first: Term, second: Term): Substitution {

            ensuringArgumentIsAtom(0)
            ensuringArgumentIsVariable(1)

            return when (val attrVal = resolveHelper(first.castToAtom().value, context)) {
                is Double -> Real.of(attrVal).mguWith(second)
                is Float -> Real.of(attrVal).mguWith(second)
                else -> {
                    throw SystemError.forUncaughtException(
                        context,
                        LuceException("Attribute is not a Double or Float")
                    )
                }
            }
        }
    }

    /**
     * resolveTruth/2
     *
     * A prolog predicate that resolves pip_identifier:attribute_identifier to the actual attribute value of type Truth
     *
     * Expected format: resolveTruth(Atom(<pip_id>:<attr_id>), Var)
     *
     * @return: the MGU of attr_val and Var iff attr_val is a Truth
     */
    object ResolveTruth : BinaryRelation.Functional<ExecutionContext>("resolveTruth") {
        override fun Solve.Request<ExecutionContext>.computeOneSubstitution(first: Term, second: Term): Substitution {

            ensuringArgumentIsAtom(0)
            ensuringArgumentIsVariable(1)

            return when (val attrVal = resolveHelper(first.castToAtom().value, context)) {
                is Boolean -> Truth.of(attrVal).mguWith(second)
                else -> {
                    throw SystemError.forUncaughtException(
                        context,
                        LuceException("Attribute is not a Boolean")
                    )
                }
            }
        }
    }

    /**
     * resolveString/2
     *
     * A prolog predicate that resolves pip_identifier:attribute_identifier to the actual String attribute value
     *
     * Expected format: resolveString(Atom(<pip_id>:<attr_id>), Var)
     *
     * @return: the MGU of attr_val and Var iff attr_val is a String
     */
    object ResolveString : BinaryRelation.Functional<ExecutionContext>("resolveString") {
        override fun Solve.Request<ExecutionContext>.computeOneSubstitution(first: Term, second: Term): Substitution {

            ensuringArgumentIsAtom(0)
            ensuringArgumentIsVariable(1)

            return when (val attrVal = resolveHelper(first.castToAtom().value, context)) {
                is String -> Atom.of(attrVal).mguWith(second)
                else -> {
                    throw SystemError.forUncaughtException(
                        context,
                        LuceException("Attribute is not a String")
                    )
                }
            }
        }
    }

    /**
     * resolveStringList/2
     *
     * A prolog predicate that resolves pip_identifier:attribute_identifier to the actual attribute value as string list
     *
     * Expected format: resolveStringList(Atom(<pip_id>:<attr_id>), Var)
     *
     * @return: the MGU of attr_val and Var iff attr_val is a string list
     */
    object ResolveStringList : BinaryRelation.Functional<ExecutionContext>("resolveStringList") {
        override fun Solve.Request<ExecutionContext>.computeOneSubstitution(first: Term, second: Term): Substitution {

            ensuringArgumentIsAtom(0)
            ensuringArgumentIsVariable(1)

            return when (val attrVal = resolveHelper(first.castToAtom().value, context)) {
                is List<*> -> {
                    val inList = attrVal.filterIsInstance<String>().map { x -> Atom.of(x) }
                    PrologList.of(inList).mguWith(second)
                }
                else -> {
                    throw SystemError.forUncaughtException(
                        context,
                        LuceException("Attribute is not a List")
                    )
                }
            }
        }
    }
}

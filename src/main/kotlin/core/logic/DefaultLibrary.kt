package core.logic

import core.control_flow_model.components.ComponentRegistry
import core.control_flow_model.components.PolicyInformationPoint
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
import it.unibo.tuprolog.solve.primitive.*
import it.unibo.tuprolog.theory.Theory
import it.unibo.tuprolog.theory.parsing.ClausesParser
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
        Intersection.descriptionPair,
        ResolveInt.descriptionPair,
        ResolveReal.descriptionPair,
        ResolveTruth.descriptionPair,
        ResolveString.descriptionPair,
        ResolveStringList.descriptionPair,
        PipIdentifier.descriptionPair,
        Decrement.descriptionPair,
        Increment.descriptionPair,
        Now.descriptionPair,
    )

    override val theory: Theory = ClausesParser.withDefaultOperators.parseTheory(
                    """
                        list_empty([]).
                        
                        within_time_interval(X, Y, Z) :- X =< Z, Z < Y.
                        
                    """.trimIndent())

    companion object {

        private fun getPip(id: String, context: ExecutionContext) : PolicyInformationPoint {
            return ComponentRegistry.policyInformationPoints[id]
                ?: throw SystemError.forUncaughtException(
                    context,
                    LuceException("PIP with id=$id is not registered")
                )
        }

        private fun splitIdentifier(value: String, context: ExecutionContext) : Pair<String, String> {
            val tokens = value.split(":")
            if (tokens.size != 2) {
                throw SyntaxError.of(
                    context,
                    "String of form <pip_identifier>:<attribute_identifier> expected, got $value"
                )
            }
            return Pair(tokens[0], tokens[1])
        }

        private fun resolveHelper(value: String, context: ExecutionContext) : Any? {
            val (pipId, attrId) = splitIdentifier(value, context)
            val pip = getPip(pipId, context)
            return pip.queryInformation(attrId)
        }

        fun intersection(a: PrologList, b: PrologList) : PrologList {
            val set = mutableSetOf<Term>()
            a.toList().iterator().forEach {
                set.add(it)
            }
            val out = mutableSetOf<Term>()
            b.toList().iterator().forEach {
                if (set.contains(it)) {
                    out.add(it)
                }
            }
            return PrologList.of(out)
        }
    }

    /**
     * pip_identifier/3
     *
     * Expected format: intersection(+Atom1, +Atom2, ?Common)
     * +Atom1: Atom
     * +Atom2: Atom
     * ?Common: Var or Atom
     *
     * A prolog predicate that unifies iff Common is the concatenation '<Atom1>:<Atom2>'
     */
    object PipIdentifier : TernaryRelation.Functional<ExecutionContext>("pip_identifier") {
        override fun Solve.Request<ExecutionContext>.computeOneSubstitution(
            first: Term,
            second: Term,
            third: Term
        ): Substitution {
            ensuringArgumentIsAtom(0)
            ensuringArgumentIsAtom(1)

            return if (third is Var || third is Atom) {
                Atom.of(first.castToAtom().value + ":" + second.castToAtom().value).mguWith(third)
            } else {
                ensuringArgumentIsVariable(2)
                Substitution.failed()
            }
        }

    }

    /**
     * intersection/3
     *
     * Expected format: intersection(+List1, +List2, ?Common)
     * +List1: List
     * +List2: List
     * ?Common: List or Var
     *
     * A prolog predicate that unifies iff Common is the intersection of List1 and List2
     */
    object Intersection : TernaryRelation.Functional<ExecutionContext>("intersection") {
        override fun Solve.Request<ExecutionContext>.computeOneSubstitution(
            first: Term,
            second: Term,
            third: Term
        ): Substitution {
            ensuringArgumentIsList(0)
            ensuringArgumentIsList(1)

            return if (third is Var || third is PrologList) {
                intersection(first.castToList(), second.castToList()).mguWith(third)
            } else {
                ensuringArgumentIsVariable(2)
                Substitution.failed()
            }
        }
    }

    /**
     * resolve_int/2
     *
     * Expected format: resolve_int(+Atom1, ?X1)
     * +Atom1: Atom with value <pip_id>:<attr_id>
     * ?X1: Var
     *
     * A prolog predicate that resolves pip_identifier:attribute_identifier to the actual attribute value of type Integer
     *
     * @return: the MGU of attr_val and Var iff attr_val is Int or Long
     */
    object ResolveInt : BinaryRelation.Functional<ExecutionContext>("resolve_int") {
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
     * resolve_real/2
     *
     * Expected format: resolve_real(+Atom1, ?X1)
     * +Atom1: Atom with value <pip_id>:<attr_id>
     * ?X1: Var
     *
     * A prolog predicate that resolves pip_identifier:attribute_identifier to the actual attribute value of type Real
     *
     * @return: the MGU of attr_val and Var iff attr_val is Real
     */
    object ResolveReal : BinaryRelation.Functional<ExecutionContext>("resolve_real") {
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
     * resolve_truth/2
     *
     * Expected format: resolve_truth(+Atom1, ?X1)
     * +Atom1: Atom with value <pip_id>:<attr_id>
     * ?X1: Var
     *
     * A prolog predicate that resolves pip_identifier:attribute_identifier to the actual attribute value of type Truth
     *
     * @return: the MGU of attr_val and Var iff attr_val is a Truth
     */
    object ResolveTruth : BinaryRelation.Functional<ExecutionContext>("resolve_truth") {
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
     * resolve_string/2
     *
     * Expected format: resolve_string(+Atom1, ?X1)
     * +Atom1: Atom with value <pip_id>:<attr_id>
     * ?X1: Var
     *
     * A prolog predicate that resolves pip_identifier:attribute_identifier to the actual String attribute value
     *
     * @return: the MGU of attr_val and Var iff attr_val is a String
     */
    object ResolveString : BinaryRelation.Functional<ExecutionContext>("resolve_string") {
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
     * resolve_string_list/2
     *
     * Expected format: resolve_string_list(+Atom1, ?X1)
     * +Atom1: Atom with value <pip_id>:<attr_id>
     * ?X1: Var
     *
     * A prolog predicate that resolves pip_identifier:attribute_identifier to the actual attribute value as string list
     *
     * @return: the MGU of attr_val and Var iff attr_val is a string list
     */
    object ResolveStringList : BinaryRelation.Functional<ExecutionContext>("resolve_string_list") {
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

    /**
     * decrement/1
     *
     * Expected format: decrement(+Atom1)
     * +Atom1: Atom with value <pip_id>:<attr_id>
     *
     * A prolog predicate that decrements pip_identifier:attribute_identifier (e.g. counter or semaphore)
     *
     * @return: true iff update succeed
     */
    object Decrement : UnaryPredicate.Predicative<ExecutionContext>("decrement") {
        override fun Solve.Request<ExecutionContext>.compute(first: Term): Boolean {
            ensuringArgumentIsAtom(0)

            val (pipId, attrId) = splitIdentifier(first.castToAtom().value, context)
            val pip = getPip(pipId, context)
            return pip.updateInformation(attrId, "decrement")
        }
    }

    /**
     * increment/1
     *
     * Expected format: increment(+Atom1)
     * +Atom1: Atom with value <pip_id>:<attr_id>
     *
     * A prolog predicate that increments pip_identifier:attribute_identifier
     *
     * @return: true iff update succeed
     */
    object Increment : UnaryPredicate.Predicative<ExecutionContext>("increment") {
        override fun Solve.Request<ExecutionContext>.compute(first: Term): Boolean {
            ensuringArgumentIsAtom(0)

            val (pipId, attrId) = splitIdentifier(first.castToAtom().value, context)
            val pip = getPip(pipId, context)
            return pip.updateInformation(attrId, "increment")
        }
    }

    /**
     * now/2
     *
     * Expected format: now(+Atom1, ?Common)
     * +Atom1: Atom with value <pip_id>
     * ?Common: Integer or Variable
     *
     * A prolog predicate that resolves the current UTC time from pip_identifier
     *
     * @return: the MGU of the current UTC time and ?Common
     */
    object Now : BinaryRelation.Functional<ExecutionContext>("now") {
        override fun Solve.Request<ExecutionContext>.computeOneSubstitution(first: Term, second: Term): Substitution {
            ensuringArgumentIsAtom(0)

            val pip = getPip(first.castToAtom().value, context)
            return when (val attrVal = pip.queryInformation("now")) {
                is Long -> Integer.of(attrVal).mguWith(second)
                else -> {
                    throw SystemError.forUncaughtException(
                        context,
                        LuceException("Return value of now() is not a Long")
                    )
                }
            }
        }
    }

}

package core.logic

import core.control_flow_model.components.ComponentRegistry
import core.control_flow_model.components.PolicyInformationPoint
import core.exceptions.LuceException
import core.notification.MonitorClient
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
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
        Decrement.descriptionPair,
        Increment.descriptionPair,
        Now.descriptionPair,
        ZeroModulo.descriptionPair,
        InDayInterval.descriptionPair,
        NotifyMonitor.descriptionPair,
        UsageNotification.descriptionPair
    )

    override val theory: Theory = ClausesParser.withDefaultOperators.parseTheory(
                    """
                        list_empty([]).
                       
                        within_interval(X, Y, Z) :- X =< Z, Z < Y.
                        
                        non_empty_intersection(X, Y) :-
                            intersection(X, Y, Z),
                            not(list_empty(Z)).        
                            
                        time_restriction(X, Y, T) :-
                            now(T, Z),
                            within_interval(X, Y, Z).
                            
                        day_time_restriction(X, Y, T, D) :-
                            now(T, Z),
                            in_day_interval(Z, X, Y, D).
                            
                        purpose_notification(T, S, O, R, M) :-
                            now(T, X),
                            usage_notification(X, S, O, R, N),
                            notify_monitor(N, M).
                            
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
/*
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
*/
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

    /**
     * mod_is_zero/2
     *
     * Expected format: now(+Integer1, +Integer2)
     * +Integer1: number
     * +Integer2: modulo group
     *
     * A prolog predicate that verifies if Integer1 mod Integer2 is zero
     *
     * @return: true iff Integer1 mod Integer2 == 0
     */
    object ZeroModulo : BinaryRelation.Predicative<ExecutionContext>("mod_is_zero") {
        override fun Solve.Request<ExecutionContext>.compute(first: Term, second: Term): Boolean {
            ensuringArgumentIsInteger(0)
            ensuringArgumentIsInteger(1)
            return (first.castToInteger().value.toLong() % second.castToInteger().value.toLong()) == 0L
        }
    }

    /**
     * in_day_interval/4
     *
     * Expected format: now(+Integer1, +Atom1, +Atom2, +List1)
     * +Integer1: current time as UTC seconds since epoch
     * +Atom1: start of daytime interval, in format HH:mm:ss, UTC
     * +Atom2: end of daytime interval, in format HH:mm:ss, UTC
     * +List1: Atom list of valid days ("Monday", ..., "Sunday")
     *
     * A prolog predicate that verifiers if current time lies within the daytime interval [Atom1, Atom2] and if
     * the day is valid
     *
     * @return: true iff day is valid and daytime interval is satisfied
     */
    object InDayInterval : QuaternaryRelation.Predicative<ExecutionContext>("in_day_interval") {
        override fun Solve.Request<ExecutionContext>.compute(
            first: Term,
            second: Term,
            third: Term,
            fourth: Term
        ): Boolean {
            ensuringArgumentIsInteger(0) // time
            ensuringArgumentIsAtom(1) // startTime in HH:mm:ss
            ensuringArgumentIsAtom(2) // endTime in HH:mm:ss
            ensuringArgumentIsList(3) // days

            // parse time from seconds since epoch (UTC)
            val time = LocalDateTime.ofEpochSecond(first.castToInteger().value.toLong(), 0, ZoneOffset.UTC)

            // parse start and end daytime
            val currentTime = time.toLocalTime()
            val currentDay = time.dayOfWeek
            val startTime = LocalTime.parse(second.castToAtom().value, DateTimeFormatter.ISO_LOCAL_TIME)
            val endTime = LocalTime.parse(third.castToAtom().value, DateTimeFormatter.ISO_LOCAL_TIME)

            // parse days
            val days = mutableListOf<DayOfWeek>()
            fourth.castToList().toList().forEach {
                if (!it.isAtom) {
                    throw SystemError.forUncaughtException(
                        context,
                        LuceException("Weekday is expected to be an Atom")
                    )
                }
                val d = when (val x = it.castToAtom().value) {
                    "Monday" -> DayOfWeek.MONDAY
                    "Tuesday" -> DayOfWeek.TUESDAY
                    "Wednesday" -> DayOfWeek.WEDNESDAY
                    "Thursday" -> DayOfWeek.THURSDAY
                    "Friday" -> DayOfWeek.FRIDAY
                    "Saturday" -> DayOfWeek.SATURDAY
                    "Sunday" -> DayOfWeek.SUNDAY
                    else -> {
                        throw SystemError.forUncaughtException(
                            context,
                            LuceException("Unknown weekday '$x'")
                        )
                    }
                }
                days.add(d)
            }

            if (!days.contains(currentDay)) {
                println("Invalid weekday")
                return false
            }

            // verify if current time is between start and end time
            return startTime.isBefore(currentTime) && endTime.isAfter(currentTime)
        }
    }

    /**
     * notify_monitor/1
     *
     * Expected format: now(+Atom1, +Atom2)
     * +Atom1: Notification
     * +Atom2: Monitor Identifier
     *
     * A prolog predicate that sends a notification to the monitor for subsequent audits
     *
     * @return: true iff notification was sent successfully
     */
    object NotifyMonitor : BinaryRelation.Predicative<ExecutionContext>("notify_monitor") {
        override fun Solve.Request<ExecutionContext>.compute(first: Term, second: Term): Boolean {
            ensuringArgumentIsAtom(0)
            ensuringArgumentIsAtom(1)

            val monitorId = second.castToAtom().value
            val monitor = MonitorClient.registry[monitorId]
                ?: throw SystemError.forUncaughtException(
                    context, LuceException("Missing notification monitor '$monitorId'")
                )

            return monitor.notify(first.castToAtom().value)
        }
    }

    /**
     * usage_notification/5
     *
     * Expected format: now(+Integer1, +Atom1, +Atom2, +Atom3, ?Common)
     *
     * A prolog predicate that creates a usage notification out of DateTime, Subject, Object and Right
     *
     * @return: MGU of notification
     */
    object UsageNotification : QuinaryRelation.Functional<ExecutionContext>("usage_notification") {
        override fun Solve.Request<ExecutionContext>.computeOneSubstitution(
            first: Term,
            second: Term,
            third: Term,
            fourth: Term,
            fifth: Term
        ): Substitution {
            ensuringArgumentIsInteger(0)
            ensuringArgumentIsAtom(1)
            ensuringArgumentIsAtom(2)
            ensuringArgumentIsAtom(3)
            ensuringArgumentIsVariable(4)

            val dateTime = LocalDateTime.ofEpochSecond(
                first.castToInteger().value.toLong(),
                0,
                ZoneOffset.UTC
            )

            val subject = second.castToAtom().value
            val obj = third.castToAtom().value
            val right = fourth.castToAtom().value

            val notification = "$dateTime: Usage of object=$obj from subject=$subject with right=$right."

            return Atom.of(notification).mguWith(fifth)
        }

    }
}

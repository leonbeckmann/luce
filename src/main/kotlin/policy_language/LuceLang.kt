package policy_language

import core.exceptions.LuceException
import core.policies.LucePolicy
import it.unibo.tuprolog.core.*
import it.unibo.tuprolog.dsl.prolog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import it.unibo.tuprolog.core.List as PrologList

class LuceLang  {

    @Serializable
    data class PolicyWrapper(val policy: Policy, val signature: String)

    @Serializable
    data class Policy(
        val id: String,
        val issuer: String,
        val contexts: Set<PolicyContext>,
        val rights: Set<String>,
        val preAccess: PreAccess,
        val ongoingAccess: OnAccess,
        val postAccess: PostAccess,
        val postRevocation: PostRevocation,
    )

    @Serializable
    sealed class PolicyContext {

        @Serializable
        @SerialName("objectId")
        data class ObjectId(val value: String) : PolicyContext()

        @Serializable
        @SerialName("objectClass")
        data class ObjectClass(val value: String) : PolicyContext()
    }

    @Serializable
    data class PreAccess(val predicates: List<Predicate>)

    @Serializable
    sealed class Trigger {

        @Serializable
        @SerialName("period")
        data class PeriodicTrigger(val period: Long) : Trigger()

    }

    @Serializable
    data class OnAccess(val triggers: List<Trigger>, val predicates: List<Predicate>)

    @Serializable
    data class PostAccess(val predicates: List<Predicate>)

    @Serializable
    data class PostRevocation(val predicates: List<Predicate>)

    @Serializable
    sealed class Predicate {

        abstract fun translate() : Struct

        @Serializable
        @SerialName("notification")
        data class Notification(
            val monitor: String,
            val message: String,
        ) : Predicate() {
            override fun translate(): Struct = prolog {
                "notify_monitor"(Atom.of(message), Atom.of(monitor))
            }
        }

        @Serializable
        @SerialName("usageNotification")
        data class UsageNotification(
            val monitor: String,
            val timePip: String,
            val subjectAttrPip: String,
            val objectAttrPip: String
        ) : Predicate() {
            override fun translate(): Struct = prolog {
                val v1 = Var.of("X")
                val v2 = Var.of("X")
                "resolve_string"(Atom.of("$subjectAttrPip:\$SUBJECT.identity"), v1) and
                "resolve_string"(Atom.of("$subjectAttrPip:\$SUBJECT.identity"), v2) and
                "purpose_notification"(Atom.of(timePip), v1, v2, Atom.of("\$RIGHT"), Atom.of(monitor))
            }
        }

        // TODO format

        @Serializable
        @SerialName("timeInterval")
        data class TimeIntervalRestriction(
            val timePip : String,
            val startTime: String,  // ISO_LOCAL_DATE_TIME: yyyy-MM-ddTHH:mm:ss
            val endTime: String,    // ISO_LOCAL_DATE_TIME
            val timeZone: String
        ) : Predicate() {
            override fun translate(): Struct {
                val startTime = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val endTime = LocalDateTime.parse(endTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val zone = ZoneId.of(timeZone)
                val zonedStart = ZonedDateTime.of(startTime, zone)
                val zonedEnd = ZonedDateTime.of(endTime, zone)

                return prolog {
                    "time_restriction"(zonedStart.toEpochSecond(), zonedEnd.toEpochSecond(), Atom.of(timePip))
                }
            }
        }

        @Serializable
        @SerialName("duration")
        data class DurationRestriction(
            val timePip : String,
            val startTime: String,  // ISO_LOCAL_DATE_TIME
            val duration: Long,     // seconds
            val timeZone: String
        ) : Predicate() {
            override fun translate(): Struct {
                val startTime = LocalDateTime.parse(startTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val endTime = startTime.plusSeconds(duration)
                val zone = ZoneId.of(timeZone)
                val zonedStart = ZonedDateTime.of(startTime, zone)
                val zonedEnd = ZonedDateTime.of(endTime, zone)

                return prolog {
                    "time_restriction"(zonedStart.toEpochSecond(), zonedEnd.toEpochSecond(), Atom.of(timePip))
                }
            }
        }

        @Serializable
        @SerialName("dayTime")
        data class DayTimeRestriction(
            val timePip: String,
            val startDayTime: String,   // ISO_LOCAL_TIME in UTC
            val endDayTime: String,     // ISO_LOCAL_TIME in UTC
            val days: List<String>
        ) : Predicate() {
            override fun translate(): Struct = prolog {
                val atomicDays = mutableListOf<Atom>()
                days.forEach { atomicDays.add(Atom.of(it)) }
                "day_time_restriction"(Atom.of(startDayTime), Atom.of(endDayTime), Atom.of(timePip), PrologList.of(atomicDays))
            }
        }

        @Serializable
        @SerialName("and")
        data class And(val first: Predicate, val second: Predicate) : Predicate() {
            override fun translate(): Struct = prolog {
                (first.translate() and second.translate())
            }
        }

        @Serializable
        @SerialName("or")
        data class Or(val first: Predicate, val second: Predicate) : Predicate() {
            override fun translate(): Struct = prolog {
                (first.translate() or second.translate())
            }
        }

        @Serializable
        @SerialName("not")
        data class Not(val value: Predicate) : Predicate() {
            override fun translate(): Struct = prolog {
                "not"(value.translate())
            }
        }

        @Serializable
        @SerialName("custom")
        data class Custom(
            val functor: String,
            val args: List<Argument>
        ) : Predicate() {
            override fun translate(): Struct = prolog {
                // each argument is translated to a term and a struct
                // the struct is added to the overall struct via AND
                // the term is used as an argument to the functor struct
                var s : Struct = Truth.TRUE
                val args = args.map { x ->
                    val (struct, t) = x.translate()
                    s = s and struct
                    t
                }
                s and Struct.of(functor, args)
            }
        }

        @Serializable
        sealed class Argument {

            /**
             * A function that translates an argument to prolog
             *
             * @return: A pair of a struct and a term. The struct is combined with the custom struct by AND, the term
             * corresponds to the argument of the custom struct
             */
            abstract fun translate() : Pair<Struct, Term>

            @Serializable
            @SerialName("resolveString")
            data class ResolveString(val pip: String, val attr: String) : Argument() {
                override fun translate(): Pair<Struct, Term> = prolog {
                    val v : Term = Var.of("X")
                    val struct = "resolve_string"(Atom.of("$pip:$attr"), v)
                    return@prolog Pair(struct, v)
                }
            }

            @Serializable
            @SerialName("resolveStringList")
            data class ResolveStringList(val pip: String, val attr: String) : Argument() {
                override fun translate(): Pair<Struct, Term> = prolog {
                    val v : Term = Var.of("X")
                    val struct = "resolve_string_list"(Atom.of("$pip:$attr"), v)
                    return@prolog Pair(struct, v)
                }
            }

            @Serializable
            @SerialName("resolveInt")
            data class ResolveInt(val pip: String, val attr: String) : Argument() {
                override fun translate(): Pair<Struct, Term> = prolog {
                    val v : Term = Var.of("X")
                    val struct = "resolve_int"(Atom.of("$pip:$attr"), v)
                    return@prolog Pair(struct, v)
                }
            }

            @Serializable
            @SerialName("resolveReal")
            data class ResolveReal(val pip: String, val attr: String) : Argument() {
                override fun translate(): Pair<Struct, Term> = prolog {
                    val v : Term = Var.of("X")
                    val struct = "resolve_real"(Atom.of("$pip:$attr"), v)
                    return@prolog Pair(struct, v)
                }
            }

            @Serializable
            @SerialName("resolveTruth")
            data class ResolveTruth(val pip: String, val attr: String) : Argument() {
                override fun translate(): Pair<Struct, Term> = prolog {
                    val v : Term = Var.of("X")
                    val struct = "resolve_truth"(Atom.of("$pip:$attr"), v)
                    return@prolog Pair(struct, v)
                }
            }

            @Serializable
            @SerialName("resolveSemaphore")
            data class ResolveSemaphore(val pip: String, val attr: String) : Argument() {
                override fun translate(): Pair<Struct, Term> {
                    return Pair(Truth.TRUE, Atom.of("$pip:$attr"))
                }
            }

            @Serializable
            @SerialName("lString")
            data class LString(val value: String) : Argument() {
                override fun translate(): Pair<Struct, Term> {
                    return Pair(Truth.TRUE, Atom.of(value))
                }
            }

            @Serializable
            @SerialName("lInt")
            data class LInt(val value: Int) : Argument() {
                override fun translate(): Pair<Struct, Term> {
                    return Pair(Truth.TRUE, Integer.of(value))
                }
            }
        }
    }

    companion object : PolicyLanguage<PolicyWrapper> {
        override fun deserialize(serialized: String) : PolicyWrapper {
            return try {
                Json.decodeFromString(serialized)
            } catch (e: Exception) {
                throw LuceException("Deserializing LuceLang failed with exception", e)
            }
        }

        private fun translationHelper(predicates: List<Predicate>) : Struct = prolog {
            if (predicates.isEmpty()) {
                // empty case
                return@prolog Truth.TRUE
            }

            // not empty, translate predicates and concatenate via AND
            val iterator = predicates.iterator()
            var s : Struct = iterator.next().translate()

            while (iterator.hasNext()) {
                s = s and iterator.next().translate()
            }

            return@prolog s
        }

        override fun translate(obj: PolicyWrapper): LucePolicy {

            val triggers = obj.policy.ongoingAccess.triggers
            var period : Long? = null
            if (triggers.size > 1) {
                throw LuceException("Multiple ongoing triggers not yet supported")
            } else if (triggers.size == 1) {
                val trigger = triggers.first()
                if (trigger is Trigger.PeriodicTrigger) {
                    period = trigger.period
                } else {
                    throw LuceException("Trigger not yet supported")
                }
            }

            return LucePolicy(
                translationHelper(obj.policy.preAccess.predicates),
                Truth.TRUE,
                translationHelper(obj.policy.ongoingAccess.predicates),
                period,
                translationHelper(obj.policy.postRevocation.predicates),
                translationHelper(obj.policy.postAccess.predicates),
            )

        }

        override fun id(): String = "luce_lang_v1"

    }

}
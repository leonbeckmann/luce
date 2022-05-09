package policy_language

import core.exceptions.LuceException
import core.policies.LucePolicy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.Semaphore

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

        @Serializable
        @SerialName("notification")
        data class Notification(
            val monitor: String,
            val message: String,
        ) : Predicate()

        @Serializable
        @SerialName("usageNotification")
        data class UsageNotification(
            val monitor: String,
            val timePip: String,
            val subjectAttrPip: String,
            val objectAttrPip: String
        ) : Predicate()

        @Serializable
        @SerialName("timeInterval")
        data class TimeIntervalRestriction(
            val timePip : String,
            val startTime: String,
            val endTime: String,
            val timeZone: String
        ) : Predicate()

        @Serializable
        @SerialName("duration")
        data class DurationRestriction(
            val timePip : String,
            val startTime: String,
            val duration: Long,
            val timeZone: String
        ) : Predicate()

        @Serializable
        @SerialName("dayTime")
        data class DayTimeRestriction(
            val timePip: String,
            val startDayTime: String,
            val endDayTime: String,
            val timeZone: String,
            val days: List<String>
        ) : Predicate()

        @Serializable
        @SerialName("and")
        data class And(val first: Predicate, val second: Predicate)

        @Serializable
        @SerialName("or")
        data class Or(val first: Predicate, val second: Predicate)

        @Serializable
        @SerialName("not")
        data class Not(val value: Predicate)

        @Serializable
        @SerialName("custom")
        data class Custom(
            val identifier: String,
            val args: List<Argument>
        ) : Predicate()

        @Serializable
        sealed class Argument {

            @Serializable
            @SerialName("resolveString")
            data class ResolveString(val pip: String, val attr: String) : Argument()

            @Serializable
            @SerialName("resolveStringList")
            data class ResolveStringList(val pip: String, val attr: String) : Argument()

            @Serializable
            @SerialName("resolveInt")
            data class ResolveInt(val pip: String, val attr: String) : Argument()

            @Serializable
            @SerialName("resolveReal")
            data class ResolveReal(val pip: String, val attr: String) : Argument()

            @Serializable
            @SerialName("resolveTruth")
            data class ResolveTruth(val pip: String, val attr: String) : Argument()

            @Serializable
            @SerialName("resolveSemaphore")
            data class ResolveSemaphore(val pip: String, val attr: String) : Argument()

            @Serializable
            @SerialName("lString")
            data class LString(val value: String) : Argument()

            @Serializable
            @SerialName("lInt")
            data class LInt(val value: Int) : Argument()
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

        override fun translate(obj: PolicyWrapper): LucePolicy {
            TODO()
        }

        override fun id(): String = "luce_lang_v1"

    }

}
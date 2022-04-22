package policies.luce_lang

import core.exceptions.LuceException
import core.policies.LucePolicy
import core.policies.PolicyLanguage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class LuceLang {

    @Serializable
    data class PolicyWrapper(val policy: Policy, val signature: String)

    @Serializable
    data class Policy(
        val issuer: String,
        val contexts: Set<PolicyContext>,
        val validity: PolicyValidity,
        val rights: Set<String>,
        val preAccess: PreAccess,
        val ongoingAccess: OnAccess,
        val postAccess: PostAccess
    )

    @Serializable
    sealed class PolicyContext

    @Serializable
    @SerialName("objectId")
    data class ObjectId(val value: String) : PolicyContext()

    @Serializable
    data class PolicyValidity(val notBefore: Long? = null, val notAfter: Long? = null)

    @Serializable
    data class PreAccess(val rules : Set<PreAccessRule>)

    @Serializable
    sealed class PreAccessRule

    @Serializable
    @SerialName("CR1")
    data class ControlRule1(val value : String? = null) : PreAccessRule()

    @Serializable
    @SerialName("UR1")
    data class UpdateRule1(val value : String? = null) : PreAccessRule()

    @Serializable
    @SerialName("DR1")
    data class DependencyRule1(val value : String? = null) : PreAccessRule()

    @Serializable
    @SerialName("UR2")
    data class UpdateRule2(val value : String? = null) : PreAccessRule()


    @Serializable
    data class OnAccess(val triggers: Set<Trigger>, val rules : Set<OnAccessRule>)

    @Serializable
    sealed class Trigger

    @Serializable
    @SerialName("period")
    data class PeriodicTrigger(val period: Long) : Trigger()

    @Serializable
    sealed class OnAccessRule

    @Serializable
    @SerialName("CR2")
    data class ControlRule2(val value : String? = null) : OnAccessRule()

    @Serializable
    @SerialName("UR3")
    data class UpdateRule3(val value : String? = null) : OnAccessRule()

    @Serializable
    @SerialName("UR4")
    data class UpdateRule4(val value : String? = null) : OnAccessRule()

    @Serializable
    @SerialName("DR2")
    data class DependencyRule2(val value : String? = null) : OnAccessRule()

    @Serializable
    data class PostAccess(val rules : Set<PostAccessRule>)

    @Serializable
    sealed class PostAccessRule

    @Serializable
    @SerialName("UR5")
    data class UpdateRule5(val value : String? = null) : PostAccessRule()

    @Serializable
    @SerialName("UR6")
    data class UpdateRule6(val value : String? = null) : PostAccessRule()

    @Serializable
    @SerialName("DR3")
    data class DependencyRule3(val value : String? = null) : PostAccessRule()

    @Serializable
    @SerialName("DR4")
    data class DependencyRule4(val value : String? = null) : PostAccessRule()

    companion object : PolicyLanguage<PolicyWrapper> {

        override fun deserialize(serialized: String) : PolicyWrapper {
            return try {
                Json.decodeFromString(serialized)
            } catch (e: Exception) {
                throw LuceException(e)
            }
        }

        override fun translate(obj: PolicyWrapper) : LucePolicy {
            TODO("Not yet implemented")
        }

        override fun id() : String {
            return "LuceLang"
        }

    }

}
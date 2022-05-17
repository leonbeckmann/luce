package policy_language

import it.unibo.tuprolog.core.Truth
import org.junit.jupiter.api.Test
import java.io.File

internal class LuceLangTest {

    @Test
    fun testPolicyTranslation() {
        val path = javaClass.classLoader.getResource("policies/policy1.json")!!.path
        val str = File(path).inputStream().readBytes().toString(Charsets.UTF_8)
        val json = LuceLang.deserialize(str)

        // deserialize
        assert(json.signature == "policySignature")
        assert(json.policy.id == "policy1")
        assert(json.policy.issuer == "issuerIdentity")
        assert(json.policy.contexts.size == 1)
        val context = json.policy.contexts.first()
        assert(context is LuceLang.PolicyContext.ObjectId && context.value == "object1")
        assert(json.policy.rights.size == 3)
        assert(json.policy.rights.containsAll(listOf("RIGHT_ID_READ", "RIGHT_ID_WRITE", "RIGHT_ID_APPEND")))
        val preAccess = json.policy.preAccess.predicates
        assert(preAccess.size == 3)
        val subjectRestriction = preAccess[0]
        assert(
            subjectRestriction is LuceLang.Predicate.Custom &&
                    subjectRestriction.functor == "member" &&
                    subjectRestriction.args.size == 2 &&
                    subjectRestriction.args.first() is LuceLang.Predicate.Argument.ResolveString &&
                    subjectRestriction.args.last() is LuceLang.Predicate.Argument.ResolveStringList
        )
        val decrement = preAccess[1]
        assert(
            decrement is LuceLang.Predicate.Custom &&
                    decrement.functor == "decrement" &&
                    decrement.args.size == 1 &&
                    decrement.args.first() is LuceLang.Predicate.Argument.ResolveSemaphore
        )
        val usageNotification = preAccess[2]
        assert(
            usageNotification is LuceLang.Predicate.UsageNotification &&
                    usageNotification.monitor == "monitor" &&
                    usageNotification.timePip == "pip_time1"
        )
        val triggers = json.policy.ongoingAccess.triggers
        assert(triggers.size == 1)
        val trigger = triggers.first()
        assert(trigger is LuceLang.Trigger.PeriodicTrigger && trigger.period == 60L)

        val onAccess = json.policy.ongoingAccess.predicates
        assert(onAccess.size == 2)
        val usageNotification2 = onAccess[0]
        assert(
            usageNotification2 is LuceLang.Predicate.UsageNotification &&
                    usageNotification2.monitor == "monitor" &&
                    usageNotification2.timePip == "pip_time1"
        )
        val postAccess = json.policy.postAccess.predicates
        assert(postAccess.size == 1)
        val increment = postAccess[0]
        assert(
            increment is LuceLang.Predicate.Custom &&
                    increment.functor == "increment" &&
                    increment.args.size == 1 &&
                    increment.args.first() is LuceLang.Predicate.Argument.ResolveSemaphore
        )
        val postRevocation = json.policy.postRevocation.predicates
        assert(postRevocation.size == 1)
        val increment2 = postRevocation[0]
        assert(
            increment2 is LuceLang.Predicate.Custom &&
                    increment2.functor == "increment" &&
                    increment2.args.size == 1 &&
                    increment2.args.first() is LuceLang.Predicate.Argument.ResolveSemaphore
        )

        // translate
        val policy = LuceLang.translate(json)
        assert(policy.preAccess.toString().contains("purpose_notification(pip_time1,"))
        assert(policy.ongoingAccess.toString().contains("purpose_notification(pip_time1,"))
        assert(policy.ongoingAccess.toString().contains("day_time_restriction('07:00:00', '18:00:00', pip_time1, ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'])"))
        assert(policy.ongoingPeriod == 60L)
        assert(policy.postPermit == Truth.TRUE)
        assert(policy.postAccessEnded.toString() == "((true, true), increment('test_pip_attr:\$OBJECT.semaphore'))")
        assert(policy.postAccessRevoked.toString() == "((true, true), increment('test_pip_attr:\$OBJECT.semaphore'))")
    }
}
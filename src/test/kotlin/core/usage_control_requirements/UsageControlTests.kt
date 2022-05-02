package core.usage_control_requirements

import core.control_flow_model.components.ComponentRegistry
import core.control_flow_model.components.PolicyInformationPoint
import core.logic.PolicyEvaluator
import it.unibo.tuprolog.dsl.prolog
import it.unibo.tuprolog.solve.SolveOptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.concurrent.Semaphore

/**
 * Testing Usage Control Requirements from Section 5
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
internal class UsageControlTests {

    class AttributePip : PolicyInformationPoint {

        val attributes = mutableMapOf<Any, Any>()

        init {
            // put example attributes
            attributes["subject1.identity"] = "subject1"
            attributes["subject1.organizations"] = listOf("org1", "org2")
            attributes["subject1.devices"] = listOf("device1", "device2")
            attributes["object1.authorized"] = listOf("subject1", "subject2", "subject3", "org2", "org3")
            attributes["object1.validDevices"] = listOf("device1")
            attributes["object1.semaphore"] = Semaphore(1)
            attributes["object1.revoked"] = false
            attributes["object1.counter"] = 5
            attributes["object1.durationIntervalStart"] = OffsetDateTime.now().toEpochSecond()
            attributes["object1.durationIntervalEnd"] = OffsetDateTime.now().toEpochSecond() + 100

        }

        override fun queryInformation(identifier: Any): Any? {
            return attributes[identifier]
        }

        override fun updateInformationByValue(identifier: Any, newValue: Any?) : Boolean = false

        override fun updateInformation(identifier: Any, description: String): Boolean {
            val attr = attributes[identifier] ?: return false

            when (attr) {
                is Int -> when (description) {
                    "increment" -> {
                        if (attr < Int.MAX_VALUE) {
                            attributes[identifier] = attr + 1
                            return true
                        }
                        return false
                    }
                    "decrement" -> {
                        if (attr > 0) {
                            attributes[identifier] = attr - 1
                            return true
                        }
                        return false
                    }
                    else -> return false
                }
                is Semaphore -> return when (description) {
                    "increment" -> {
                        attr.release()
                        true
                    }
                    "decrement" -> {
                        attr.acquire()
                        true
                    }
                    else -> false
                }
                else -> return false
            }
        }
    }

    class DevicePip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): String {
            return "device1"
        }

        override fun updateInformationByValue(identifier: Any, newValue: Any?) = false
        override fun updateInformation(identifier: Any, description: String): Boolean = false
    }

    class TimePip : PolicyInformationPoint {
        override fun queryInformation(identifier: Any): Long {
            return OffsetDateTime.now().toEpochSecond()
        }

        override fun updateInformationByValue(identifier: Any, newValue: Any?): Boolean = false

        override fun updateInformation(identifier: Any, description: String): Boolean = false

    }

    companion object {

        @BeforeAll
        @JvmStatic
        fun registerPips() {
            ComponentRegistry.addPolicyInformationPoint("test_pip_attr", AttributePip())
            ComponentRegistry.addPolicyInformationPoint("test_pip_device", DevicePip())
            ComponentRegistry.addPolicyInformationPoint("test_pip_time", TimePip())
        }

    }

    /**
     * U1: Restriction to Subjects
     */

    @Test
    fun testSubjectRestriction() {
        // s.identity in o.authorized
        val solution = PolicyEvaluator.evaluate(
            prolog {
                "resolve_string"("test_pip_attr:subject1.identity", "X") and
                "resolve_string_list"("test_pip_attr:object1.authorized", "Y") and
                "member"("X", "Y")
            },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testOrganizationRestriction() {
        // s.organizations intersect o.authorized not empty
        val solution = PolicyEvaluator.evaluate(
            prolog {
                "resolve_string_list"("test_pip_attr:subject1.organizations", "X") and
                "resolve_string_list"("test_pip_attr:object1.authorized", "Y") and
                "intersection"("X", "Y", "Z") and
                "not"("list_empty"("Z"))
            },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    // TODO Role-based Access Control

    /**
     * U2 - Restriction to Devices
     */

    @Test
    fun testDeviceRestrictions() {
        // deviceIdentity in s.devices and deviceIdentity in o.validDevices
        val solution = PolicyEvaluator.evaluate(
            prolog {
                "resolve_string"("test_pip_device:identity", "X") and
                "resolve_string_list"("test_pip_attr:subject1.devices", "Y") and
                "resolve_string_list"("test_pip_attr:object1.validDevices", "Z") and
                "member"("X", "Y") and "member"("X", "Z")
            },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    @Test
    fun testDevicesLimit() {
        // model usage via semaphore

        val attributes = (ComponentRegistry.policyInformationPoints["test_pip_attr"]!! as AttributePip).attributes
        val semaphore = attributes["object1.semaphore"] as Semaphore

        var solution = PolicyEvaluator.evaluate(
            prolog {
                "decrement"("test_pip_attr:object1.semaphore")
            },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
        assert(!semaphore.tryAcquire())

        solution = PolicyEvaluator.evaluate(
            prolog {
                "increment"("test_pip_attr:object1.semaphore")
            },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
        assert(semaphore.tryAcquire())
        semaphore.release()
    }

    /**
     * U3 - Restriction to Applications
     */

    @Test
    fun testApplicationRestriction() {
        // check if targetApp or its class is allowed to use the object
        // TODO resolve flow context from X
        val solution = PolicyEvaluator.evaluate(
            prolog { "not"(false) },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    /**
     * U5 - Environmental Conditions
     */

    @Test
    fun testTime() {
        // check if now() is in allowed interval
        val solution = PolicyEvaluator.evaluate(
            prolog {
                "resolve_int"("test_pip_attr:object1.durationIntervalStart", "X") and
                "resolve_int"("test_pip_attr:object1.durationIntervalEnd", "Y") and
                "now"("test_pip_time", "Z") and
                "within_time_interval"("X", "Y", "Z")
            },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    // TODO
     // DayTime timestamps
    // getDayTime()
    // o.timeIntervalStart, o.timeIntervalEnd

    // TODO location-based access

    /**
     * U7 - Usage Access Revocation
     */

    @Test
    fun testRevocation() {
        // not o.revoked
        val solution = PolicyEvaluator.evaluate(
            prolog {
                "resolve_truth"("test_pip_attr:object1.revoked", "X") and "not"("X")
            },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    /**
     * U8 - Occurrence of Events
     */
    // TODO
    // pay_provider(s.credits, o.one_time_fee)
    // when first of month then pay_provider_monthly(s.credits, o.monthly_fee)

    // Notification(s,o,r)
    // notify_monitor(subject, notification) : Boolean

    /**
     * U9 - Purpose of Usage
     */
    // TODO
    // wait until requested usage has been allowed
    // reactive enforcement by usage notifications

    /**
     * U10 - Cardinality Limitation
     */

    // TODO if now() mod 60 == 0 then update
    @Test
    fun testDecrementCounter() {
        // decrement counter to zero
        val attributes = (ComponentRegistry.policyInformationPoints["test_pip_attr"]!! as AttributePip).attributes
        val counter = attributes["object1.counter"] as Int

        val solution = PolicyEvaluator.evaluate(
            prolog {
                "decrement"("test_pip_attr:object1.counter")
            },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
        assert((attributes["object1.counter"] as Int) == counter - 1)
    }

    @Test
    fun testPositiveCounter() {
        // o.counter > 0
        val solution = PolicyEvaluator.evaluate(
            prolog {
                "resolve_int"("test_pip_attr:object1.counter", "X") and ">"("X", 0)
            },
            SolveOptions.DEFAULT
        )
        assert(solution.isYes)
    }

    /**
     * U11 - Modification of Data
     */

    /**
     * U12 - Deletion after Usage
     */

}
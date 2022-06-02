package core.control_flow_model.components

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Component Registry
 *
 * Provides registered components (PIPs, PMP) to the state-less PDP.
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
object ComponentRegistry {

    private val LOG = LoggerFactory.getLogger(ComponentRegistry::class.java)

    lateinit var policyManagementPoint: PolicyManagementPoint

    val policyInformationPoints = ConcurrentHashMap<String, PolicyInformationPoint>()

    fun addPolicyInformationPoint(id: String, pip: PolicyInformationPoint) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Register PIP with id=$id")
        }
        policyInformationPoints[id] = pip
    }

    fun removePolicyInformationPoint(id: String) {
        if (LOG.isTraceEnabled) {
            LOG.trace("Unregister PIP with id=$id")
        }
        policyInformationPoints.remove(id)
    }

}
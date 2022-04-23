package core.control_flow_model.components

import java.util.concurrent.ConcurrentHashMap

/**
 * Component Registry
 *
 * Provides registered components (PIPs, PMP) to the state-less PDP.
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
object ComponentRegistry {

    lateinit var policyManagementPoint: PolicyManagementPoint

    val policyInformationPoints = ConcurrentHashMap<String, PolicyInformationPoint>()

    fun addPolicyInformationPoint(id: String, pip: PolicyInformationPoint) {
        policyInformationPoints[id] = pip
    }

}
package core.control_flow_model.components

import core.policies.LucePolicy

/**
 * Policy Management Point (PMP)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyManagementPoint {

    fun deploy(serialized: String, policyType: String)

    fun pull() : LucePolicy?

}
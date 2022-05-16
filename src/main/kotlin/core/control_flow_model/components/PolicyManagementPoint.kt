package core.control_flow_model.components

import core.admin.LuceObject
import core.admin.LuceRight
import core.policies.LucePolicy

/**
 * LUCE PMP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyManagementPoint {

    /**
     * A function to pull a list of applicable policies from the PMP
     *
     * @param obj: LuceObject
     * @param right: LuceRight
     * @return A list of applicable Luce Policies
     */
    fun <Sid, Oid> pullPolicy(obj : LuceObject<Sid, Oid>, right: LuceRight) : List<LucePolicy>

}
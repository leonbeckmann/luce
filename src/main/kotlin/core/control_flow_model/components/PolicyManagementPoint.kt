package core.control_flow_model.components

import core.admin.LuceObject
import core.admin.LuceRight
import core.policies.LucePolicy

/**
 * LUCE PMP, as defined in LUCE's control flow model (see Section 6.1.1)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyManagementPoint {

    /**
     * A function to pull a list of applicable policies from the PMP
     *
     * The list should be empty if no applicable policy is deployed.
     * The list should have at most size one if 'Deny-overrides' Policy Combination is used.
     * The list should have at most size one if 'Only-one-applicable' Policy Combination is used
     *
     * @param obj: LuceObject
     * @param right: LuceRight
     * @return A list of applicable Luce Policies
     */
    fun <Sid, Oid> pullPolicy(obj : LuceObject<Sid, Oid>, right: LuceRight) : List<LucePolicy>

}
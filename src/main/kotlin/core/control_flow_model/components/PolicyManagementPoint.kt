package core.control_flow_model.components

import core.policies.LucePolicy

/**
 * LUCE PMP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyManagementPoint {

    fun pullPolicy() : LucePolicy?

}
package core

import core.control_flow_model.components.PolicyManagementPoint

class LuceConfiguration {
    lateinit var policyManagementPoint: PolicyManagementPoint
        private set

    class Builder {
        private val config = LuceConfiguration()

        fun setPolicyManagementPoint(policyManagementPoint: PolicyManagementPoint): Builder {
            config.policyManagementPoint = policyManagementPoint
            return this
        }

        fun build(): LuceConfiguration {
            return config
        }
    }
}
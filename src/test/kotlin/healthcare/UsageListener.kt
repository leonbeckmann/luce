package healthcare

import core.control_flow_model.components.PolicyEnforcementPoint
import core.control_flow_model.messages.RevocationResponse

class UsageListener : PolicyEnforcementPoint {

    private lateinit var handle: RecordHandle

    fun setHandle(recordHandle: RecordHandle) {
        this.handle = recordHandle
    }

    override fun onRevocation(response: RevocationResponse) {
        handle.drop(true)
    }
}
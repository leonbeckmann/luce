package healthcare

import core.control_flow_model.components.PolicyEnforcementPoint
import core.control_flow_model.messages.RevocationResponse

class UsageListener : PolicyEnforcementPoint {

    private lateinit var handle: RecordHandle
    private lateinit var delete: () -> Boolean

    fun setHandle(recordHandle: RecordHandle) {
        this.handle = recordHandle
    }

    fun setDeletionProcedure(f: () -> Boolean) {
        delete = f
    }

    override fun onRevocation(response: RevocationResponse) {
        assert(response.solution.isYes)
        handle.drop(true)
    }

    override fun doDependency(dependencyId: String): Boolean {
        assert(dependencyId == "delete_local")
        return delete()
    }
}
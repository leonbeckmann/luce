package healthcare

import core.admin.LuceRight
import core.control_flow_model.components.PolicyDecisionPoint
import core.control_flow_model.messages.EndRequest
import java.util.concurrent.locks.ReentrantLock

class RecordHandle(
    data: Data
) {

    data class Data(val subject: HealthcareSubject, val record: PatientRecord, val right: LuceRight)

    private val lock = ReentrantLock(true)
    private var data : Data? = data

    /**
     * Delegate a right for this record iff this handle is issued for right delegation
     */
    fun delegateRight(identity: String, right: LuceRight) : Boolean {
        var result = false
        lock.lock()
        if (data != null && data!!.right == LuceRight(PatientRecord.RECORD_RIGHT_ID_DELEGATE_RIGHT)) {
            result = data!!.record.rights.getOrPut(identity) { mutableSetOf() }.add(right)
        }
        lock.unlock()
        return result
    }

    fun recordId() : String? {
        var res: String? = null;
        lock.lock()
        if (data != null)
            res = data!!.record.identity
        lock.unlock()
        return res
    }

    /**
     * End the usage or make it unusable after revocation
     */
    fun drop(revoked: Boolean) {
        lock.lock()
        if (data != null) {

            if (!revoked) {
                // create request
                val request = EndRequest(data!!.subject, data!!.record, data!!.right)

                // end usage
                PolicyDecisionPoint.endUsage(request)
            }

            // make handle unusable
            data = null
        }
        lock.unlock()
    }

    fun isRevoked() : Boolean = data == null

}
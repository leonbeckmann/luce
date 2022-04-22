package healthcare

import core.LuceRight
import core.LuceSubject
import core.control_flow_model.components.PolicyDecisionPoint
import core.control_flow_model.components.PolicyManagementPoint
import core.control_flow_model.messages.DecisionRequest
import core.exceptions.LuceException
import core.policies.LucePolicy
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import java.security.cert.X509Certificate
import java.util.UUID

class HealthManagementFacility(
    identity: X509Certificate,
) : LuceSubject<X509Certificate>(identity, identity, persistentSetOf()), PolicyManagementPoint {

    private val records = mutableMapOf<X509Certificate, PatientRecord>()

    fun registerSubject(identity: X509Certificate) : HealthcareSubject {
        // in a real world application, this has to be replaced by a signing request
        return HealthcareSubject(identity, this.identity)
    }

    fun registerEmptyRecord(identity: X509Certificate) : Boolean {
        val record = PatientRecord(UUID.randomUUID().toString(), identity)
        return records.putIfAbsent(identity, record) == null
    }

    // access request as in Figure 6.3
    fun accessRecord(
        subject : HealthcareSubject,
        recordOwner: X509Certificate,
        right: LuceRight,
    ) : RecordHandle {
        val record = records[recordOwner] ?: throw LuceException("Record not available")

        // create decision request
        val request = DecisionRequest(subject, record, right)

        // make decision
        val response = PolicyDecisionPoint.requestDecision(request)

        // TODO evaluate response

        return RecordHandle(record)
    }

    /**
     * Health Management Facility acts also as PMP
     */
    override fun deploy(serialized: String, policyType: String) {
        TODO("Not yet implemented")
    }

    override fun pull(): LucePolicy? {
        return null
    }

}
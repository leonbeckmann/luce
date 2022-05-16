package healthcare

import core.admin.LuceObject
import core.admin.LuceRight
import core.admin.LuceSubject
import core.control_flow_model.components.PolicyDecisionPoint
import core.control_flow_model.components.PolicyEnforcementPoint
import core.control_flow_model.components.PolicyInformationPoint
import core.control_flow_model.components.PolicyManagementPoint
import core.control_flow_model.messages.DecisionRequest
import core.policies.LucePolicy
import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.core.Truth
import it.unibo.tuprolog.dsl.prolog
import java.security.cert.X509Certificate
import java.util.UUID

/**
 *
 */
class HealthManagementFacility(
    identity: X509Certificate
) : LuceSubject<X509Certificate>(identity, identity, setOf()), PolicyManagementPoint, PolicyInformationPoint {

    // maps patient identity to its record
    private val recordRegistry = mutableMapOf<X509Certificate, PatientRecord>()

    // maps patient identity to subject instance
    private val subjectRegistry = mutableMapOf<X509Certificate, HealthcareSubject>()

    // policy registry, maps 'objectId:rightId' to list of applicable policies
    private val policyRegistry = mutableMapOf<String, MutableList<LucePolicy>>()

    private val attributes = mutableMapOf<String, Any>()

    /**
     * Subject Registration via Root Authority
     */
    fun registerSubject(identity: X509Certificate) : HealthcareSubject {
        // in a real world application, this has to be replaced by a signed request
        val subject = HealthcareSubject(identity, this.identity)
        subjectRegistry[identity] = subject
        return subject
    }

    /**
     * Universal right createObject
     */
    fun createEmptyRecord(identity: X509Certificate) : Boolean {
        // in a real world application, this has to be replaced by a signed request

        // create record
        val record = PatientRecord(UUID.randomUUID().toString(), identity)

        // insert into registry
        if (recordRegistry.putIfAbsent(identity, record) != null) {
            return false
        }

        // create trivial policy for this record, which allows the owner to access the record
        val struct = prolog {
            "is_authorized_by_right"(Atom.of("\$SUBJECT"), Atom.of("\$RIGHT"), "attr_pip:\$OBJECT.rights")
        }

        val policy = LucePolicy(
            preAccess = struct,
            postPermit = Truth.TRUE,        // no post-permit actions
            ongoingAccess = struct,
            ongoingPeriod = 1000,
            postAccessRevoked = Truth.TRUE, // no post-revocation actions
            postAccessEnded = Truth.TRUE    // no post-usage actions
        )

        // register policy for each right
        policyRegistry.getOrPut("${record.identity}:${PatientRecord.RECORD_RIGHT_ID_READ}") { mutableListOf() }.add(policy)
        policyRegistry.getOrPut("${record.identity}:${PatientRecord.RECORD_RIGHT_ID_APPEND}") { mutableListOf() }.add(policy)
        policyRegistry.getOrPut("${record.identity}:${PatientRecord.RECORD_RIGHT_ID_CREATE_POLICY}") { mutableListOf() }.add(policy)
        policyRegistry.getOrPut("${record.identity}:${PatientRecord.RECORD_RIGHT_ID_READ_PARTIAL}") { mutableListOf() }.add(policy)
        policyRegistry.getOrPut("${record.identity}:${PatientRecord.RECORD_RIGHT_ID_DELEGATE_RIGHT}") { mutableListOf() }.add(policy)
        policyRegistry.getOrPut("${record.identity}:${PatientRecord.RECORD_RIGHT_ID_DELETE_LOCAL}") { mutableListOf() }.add(policy)

        // create attributes
        attributes["${record.identity}.rights"] = record.rights

        return true
    }

    /**
     * Universal right assignRole
     */
    fun assignRole(organization: X509Certificate, employee: X509Certificate, role: String) : Boolean {
        // in a real world application, this has to be replaced by a signed (by organization) request
        val org = subjectRegistry[organization] ?: return false
        val s = subjectRegistry[employee] ?: return false
        return s.assignedRoles.add("${org.identity.subjectUniqueID}:$role")
    }

    /**
     *
     */
    fun accessRecord(
        identity: X509Certificate,
        recordOwner: X509Certificate,
        right: LuceRight,
        listener: PolicyEnforcementPoint
    ) : RecordHandle {

        // get subject
        val subject = subjectRegistry[identity] ?: throw HealthcareException("Unknown subject")

        // get record
        val record = recordRegistry[recordOwner] ?: throw HealthcareException("Record not available")

        // build decision request
        val request = DecisionRequest(subject, record, right, listener)

        // make decision
        val response = PolicyDecisionPoint.requestDecision(request)

        if (response.isInUse()) {
            throw HealthcareException("Already in use")
        } else if (response.isDenied()) {
            throw HealthcareException("Usage denied")
        } else {
            // usage permitted
            return RecordHandle(RecordHandle.Data(subject, record, right))
        }
    }

    /*
     * PMP functionality
     */

    /**
     * PDP pulls policy from PMP
     */
    override fun <Sid, Oid> pullPolicy(obj: LuceObject<Sid, Oid>, right: LuceRight): List<LucePolicy> {
        // we only support Permit-Override here
        return policyRegistry["${obj.identity.toString()}:${right.id}"] ?: listOf()
    }

    /**
     * Deploy serialized LuceLang policy
     */
    fun deployPolicy() {
        // in a real world application, replay protection must be added
        TODO("Not yet implemented")
    }

    /*
     * Attribute PIP functionality
     */

    /**
     *  Return attribute values
     */
    override fun queryInformation(identifier: Any): Any? {
        return attributes[identifier]
    }

    /**
     *
     */
    override fun updateInformation(identifier: Any, description: String, value: Any?): Boolean {
        TODO("Not yet implemented")
    }

}
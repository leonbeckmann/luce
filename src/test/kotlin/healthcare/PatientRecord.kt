package healthcare

import core.LuceObject
import core.LuceRight
import kotlinx.collections.immutable.persistentSetOf
import java.security.cert.X509Certificate

class PatientRecord(
    identity: String,
    owner: X509Certificate
) : LuceObject<X509Certificate, String>(identity, owner, "Record", recordRights) {

    private var content = mutableMapOf<String, String>()

    /**
     * Logic of record access rights
     */
    private fun read() : String {
        var res = ""
        for (s in content.entries) {
            res += s.key + "={" + s.value + "}\n"
        }
        return res
    }

    private fun readPartial(section: String) : String {
        return content.getOrDefault(section, "Section not available")
    }

    private fun append(section: String, data: String) {
        val c = content.getOrDefault(section, "")
        content[section] = c.plus(data)
    }

    private fun delegateRight(delegatee: HealthcareSubject, right: LuceRight) {
        // TODO ensure delegator owns rights that should be delegated
        val rights = this.rights.getOrDefault(delegatee.identity, mutableSetOf())
        rights.add(right)
        this.rights[delegatee.identity] = rights
    }

    private fun deleteLocal() {
        content.clear()
    }

    companion object {
        const val RECORD_RIGHT_ID_READ = "read"
        const val RECORD_RIGHT_ID_READ_PARTIAL = "read_partial"
        const val RECORD_RIGHT_ID_APPEND = "append"
        const val RECORD_RIGHT_ID_CREATE_POLICY = "create_policy"
        const val RECORD_RIGHT_ID_DELEGATE_RIGHT = "delegate_right"
        const val RECORD_RIGHT_ID_DELETE_LOCAL = "delete_local"

        val recordRights = persistentSetOf(
            LuceRight(RECORD_RIGHT_ID_READ),
            LuceRight(RECORD_RIGHT_ID_READ_PARTIAL),
            LuceRight(RECORD_RIGHT_ID_APPEND),
            LuceRight(RECORD_RIGHT_ID_CREATE_POLICY),
            LuceRight(RECORD_RIGHT_ID_DELEGATE_RIGHT),
            LuceRight(RECORD_RIGHT_ID_DELETE_LOCAL)
        )
    }
}
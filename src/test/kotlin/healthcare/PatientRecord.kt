package healthcare

import core.admin.LuceObject
import core.admin.LuceRight
import java.security.cert.X509Certificate

class PatientRecord(
    identity: String,
    owner: X509Certificate
) : LuceObject<X509Certificate, String>(identity, owner, "Record", recordRights) {

    companion object {
        const val RECORD_RIGHT_ID_READ = "read"
        const val RECORD_RIGHT_ID_READ_PARTIAL = "read_partial"
        const val RECORD_RIGHT_ID_APPEND = "append"
        const val RECORD_RIGHT_ID_CREATE_POLICY = "create_policy"
        const val RECORD_RIGHT_ID_DELEGATE_RIGHT = "delegate_right"
        const val RECORD_RIGHT_ID_DELETE_LOCAL = "delete_local"

        val recordRights = setOf(
            LuceRight(RECORD_RIGHT_ID_READ),
            LuceRight(RECORD_RIGHT_ID_READ_PARTIAL),
            LuceRight(RECORD_RIGHT_ID_APPEND),
            LuceRight(RECORD_RIGHT_ID_CREATE_POLICY),
            LuceRight(RECORD_RIGHT_ID_DELEGATE_RIGHT),
            LuceRight(RECORD_RIGHT_ID_DELETE_LOCAL)
        )
    }
}
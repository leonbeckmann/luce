package healthcare

import core.admin.LuceRight
import core.admin.LuceSubject
import java.security.cert.X509Certificate

class HealthcareSubject(
    identity: String,
    owner: String,
    val cert: X509Certificate
) : LuceSubject<String>(identity, owner, subjectRights) {

    val assignedRoles = mutableSetOf<String>()

    companion object {
        val subjectRights = setOf<LuceRight>()
    }
}
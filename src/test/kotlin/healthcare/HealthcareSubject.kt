package healthcare

import core.admin.LuceRight
import core.admin.LuceSubject
import java.security.cert.X509Certificate

class HealthcareSubject(
    identity: X509Certificate,
    owner: X509Certificate
) : LuceSubject<X509Certificate>(identity, owner, subjectRights) {

    val assignedRoles = mutableSetOf<String>()
    val activeRoles = mutableSetOf<String>()

    companion object {
        val subjectRights = setOf<LuceRight>()
    }
}
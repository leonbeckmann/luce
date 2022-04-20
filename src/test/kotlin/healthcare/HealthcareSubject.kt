package healthcare

import core.LuceRight
import core.LuceSubject
import kotlinx.collections.immutable.persistentSetOf
import java.security.cert.X509Certificate

class HealthcareSubject(
    identity: X509Certificate,
    owner: X509Certificate
) : LuceSubject<X509Certificate>(identity, owner, subjectRights) {

    private val roles = mutableMapOf<X509Certificate, MutableSet<String>>()

    // universal right assignRole
    fun assignRole(target: HealthcareSubject, role: String) {
        target.roles.putIfAbsent(this.identity, mutableSetOf())
        target.roles[this.identity]!!.add(role)
    }

    companion object {
        val subjectRights = persistentSetOf<LuceRight>()
    }
}
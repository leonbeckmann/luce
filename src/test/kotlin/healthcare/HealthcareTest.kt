package healthcare

import core.LuceConfiguration
import core.LuceRight
import core.control_flow_model.components.PolicyDecisionPoint
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.FileInputStream
import java.security.SignatureException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.OffsetDateTime

/**
 * Representing the Healthcare Test from Section 5.2.2:
 *
 * "The patient Alice is treated in the hospital. For this she
 * allows the nurses and the doctors to read restricted parts
 * of her patient record. Further,the doctors are allowed to
 * append Alice’s record. For emergencies, Alice names Bob as
 * her legal representative. Due to a complication during
 * surgery, the doctor requires read access to the complete
 * patient record, to check past diseases. Since Alice is
 * under anesthesia, Bob as her legal representative has to
 * allow the doctors to read the complete record for 24 hours,
 * on condition that the local copy is deleted again after no
 * later than 24 hours. All participants trust the state health
 * management facility."
 *
 * From Section 4.1.:
 * Records are created by the patient itself. Records and
 * policies are stored centrally at the health management
 * facility, which sends policies together with the records
 * on request.
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class HealthcareTest {

    /**
     * Read X509Certificate from test resource folder
     */
    private fun readCertificate(certPath: String) : X509Certificate {
        val factory = CertificateFactory.getInstance("X.509")
        val input = FileInputStream(javaClass.classLoader.getResource(certPath)!!.path)
        return factory.generateCertificate(input) as X509Certificate
    }

    // OffsetDateTime.now().toEpochSecond()

    @Test
    fun integrationTest() {

        // create root authority: health management facility
        val healthMgmtFacility = HealthManagementFacility(readCertificate("identities/ca.crt"))
        healthMgmtFacility.identity.verify(healthMgmtFacility.identity.publicKey)

        // register healthMgmtFacility as PMP at PDP
        val config = LuceConfiguration.Builder()
            .setPolicyManagementPoint(healthMgmtFacility)
            .build()

        PolicyDecisionPoint.configure(config)

        // create hospital organization
        val hospital = healthMgmtFacility.registerSubject(readCertificate("identities/hospital.crt"))
        hospital.identity.verify(healthMgmtFacility.identity.publicKey)

        // create hospital employees (Doctor, Nurse)
        val doctor = healthMgmtFacility.registerSubject(readCertificate("identities/doctor.crt"))
        val nurse = healthMgmtFacility.registerSubject(readCertificate("identities/nurse.crt"))
        doctor.identity.verify(healthMgmtFacility.identity.publicKey)
        nurse.identity.verify(healthMgmtFacility.identity.publicKey)

        // create patient Alice
        val alice = healthMgmtFacility.registerSubject(readCertificate("identities/alice.crt"))
        alice.identity.verify(healthMgmtFacility.identity.publicKey)

        // create legal representative Bob
        val bob = healthMgmtFacility.registerSubject(readCertificate("identities/bob.crt"))
        bob.identity.verify(healthMgmtFacility.identity.publicKey)

        assertThrows<SignatureException> {
            bob.identity.verify(alice.identity.publicKey)
        }

        // assign doctor and nurse to hospital organization
        hospital.assignRole(doctor, "Doctor")
        hospital.assignRole(nurse, "Nurse")

        // create Alice's patient record
        assert(healthMgmtFacility.registerEmptyRecord(alice.identity))

        // ensure Alice can read her record
        val aliceReadHandle = healthMgmtFacility.accessRecord(
            alice, alice.identity, LuceRight(PatientRecord.RECORD_RIGHT_ID_READ))
        assert(aliceReadHandle.drop())
        //assert(!aliceReadHandle.drop())

        // ensure Bob cannot read Alice's record
        //assertThrows<LuceException> {
        //    healthMgmtFacility.accessRecord(bob, alice.identity)
        //}

        // ensure Doctor cannot read Alice's record
        //assertThrows<LuceException> {
        //    healthMgmtFacility.accessRecord(doctor, alice.identity)
        //}

        // Alice creates policy for her record

        // ensure Doctor can append and partially read the record

        // ensure Doctor cannot read the record fully

        // Alice delegates rights to Bob

        // Bob creates policy that allows the doctor to read the record for 10 seconds,
        // under the condition that local copies are deleted afterwards

        // ensure doctor can read the full record

        // TODO handle local delete

        // ensure doctor cannot read again after 10 seconds

    }
}
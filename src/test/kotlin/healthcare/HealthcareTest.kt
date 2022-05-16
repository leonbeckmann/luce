package healthcare

import core.admin.LuceRight
import core.control_flow_model.components.ComponentRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.FileInputStream
import java.security.SignatureException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Representing the Healthcare Test from Section 5.2.2:
 *
 * "The patient Alice is treated in the hospital. For this she
 * allows the nurses and the doctors to read restricted parts
 * of her patient record. Further, the doctors are allowed to
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

    @Test
    fun integrationTest() {

        // create the root authority: health management facility
        val healthMgmtFacility = HealthManagementFacility(readCertificate("identities/ca.crt"))
        healthMgmtFacility.identity.verify(healthMgmtFacility.identity.publicKey)

        // register healthMgmtFacility as PMP and attr PIP
        ComponentRegistry.policyManagementPoint = healthMgmtFacility
        ComponentRegistry.addPolicyInformationPoint("attr_pip", healthMgmtFacility)

        // create hospital
        val hospital = healthMgmtFacility.registerSubject(readCertificate("identities/hospital.crt"))
        hospital.identity.verify(healthMgmtFacility.identity.publicKey)

        // create hospital employees (Doctor, Nurse)
        val doctor = healthMgmtFacility.registerSubject(readCertificate("identities/doctor.crt"))
        doctor.identity.verify(healthMgmtFacility.identity.publicKey)
        val nurse = healthMgmtFacility.registerSubject(readCertificate("identities/nurse.crt"))
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
        assert(healthMgmtFacility.assignRole(hospital.identity, doctor.identity, "Doctor"))
        assert(healthMgmtFacility.assignRole(hospital.identity, nurse.identity, "Nurse"))

        // create empty record for alice
        assert(healthMgmtFacility.createEmptyRecord(alice.identity))

        // ensure alice can read her record, i.e. no exception
        val listenerAliceRead = UsageListener()
        val handleAliceRead = healthMgmtFacility.accessRecord(
            alice.identity,
            alice.identity,
            LuceRight(PatientRecord.RECORD_RIGHT_ID_READ),
            listenerAliceRead
        )
        listenerAliceRead.setHandle(handleAliceRead)
        handleAliceRead.drop(false)

        // ensure bob cannot read alice's record yet
        assertThrows<HealthcareException> {
            healthMgmtFacility.accessRecord(
                bob.identity,
                alice.identity,
                LuceRight(PatientRecord.RECORD_RIGHT_ID_READ),
                UsageListener()
            )
        }

        // ensure doctor cannot read alice's record yet
        assertThrows<HealthcareException> {
            healthMgmtFacility.accessRecord(
                doctor.identity,
                alice.identity,
                LuceRight(PatientRecord.RECORD_RIGHT_ID_READ),
                UsageListener()
            )
        }

        // TODO
        // deploy policy that allows doctor and nurse to access the record

        // ensure doctor can append and partially read, but cannot fully read

        // delegate rights to Bob

        // ensure Bob can deploy policy that allows the doctor to read the record for 10 seconds under the condition
        // that local copies are deleted afterwards

        // ensure doctor can fully read the record

        // ensure the local copy is deleted after the usage

        // ensure the doctor cannot read the full record anymore, since usage interval is over

    }
}
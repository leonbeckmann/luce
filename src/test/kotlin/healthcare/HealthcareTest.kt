package healthcare

import core.admin.LuceRight
import core.control_flow_model.components.ComponentRegistry
import core.control_flow_model.components.PolicyInformationPoint
import core.notification.MonitorClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import policy_language.LuceLang
import java.io.FileInputStream
import java.security.SignatureException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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

    /*
    companion object {
        private val LOG = LoggerFactory.getLogger(HealthcareTest::class.java)
    }
    */

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

        // enable trace logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")

        // create the root authority: health management facility
        // LOG.info("Create healthcare management facility")
        val healthMgmtFacility = HealthManagementFacility(readCertificate("identities/ca.crt"))
        healthMgmtFacility.cert.verify(healthMgmtFacility.cert.publicKey)

        // register healthMgmtFacility as PMP and attr PIP
        // LOG.info("Register PMP, PIPs and Monitor Services")
        ComponentRegistry.policyManagementPoint = healthMgmtFacility
        ComponentRegistry.addPolicyInformationPoint("attr_pip", healthMgmtFacility)
        ComponentRegistry.addPolicyInformationPoint("time_pip", object : PolicyInformationPoint {
            override fun queryInformation(identifier: Any): Long {
                return LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC)
            }

            override fun updateInformation(identifier: Any, description: String, value: Any?): Boolean = false
        })
        MonitorClient.register("default_notification_service", object : MonitorClient {
            override fun notify(notification: String): Boolean {
                println("Notification from default_notification_service: $notification")
                return true
            }
        })

        // create hospital
        // LOG.info("Create hospital")
        val hospital = healthMgmtFacility.registerSubject("Hospital", readCertificate("identities/hospital.crt"))
        hospital.cert.verify(healthMgmtFacility.cert.publicKey)

        // create hospital employees (Doctor, Nurse)
        // LOG.info("Create Doctor and Nurse")
        val doctor = healthMgmtFacility.registerSubject("Doctor1", readCertificate("identities/doctor.crt"))
        doctor.cert.verify(healthMgmtFacility.cert.publicKey)
        val nurse = healthMgmtFacility.registerSubject("Nurse1", readCertificate("identities/nurse.crt"))
        nurse.cert.verify(healthMgmtFacility.cert.publicKey)

        // create patient Alice
        // LOG.info("Create Alice")
        val alice = healthMgmtFacility.registerSubject("Alice", readCertificate("identities/alice.crt"))
        alice.cert.verify(healthMgmtFacility.cert.publicKey)

        // create legal representative Bob
        // LOG.info("Create legal representative Bob")
        val bob = healthMgmtFacility.registerSubject("Bob", readCertificate("identities/bob.crt"))
        bob.cert.verify(healthMgmtFacility.cert.publicKey)

        assertThrows<SignatureException> {
            bob.cert.verify(alice.cert.publicKey)
        }

        // assign doctor and nurse to hospital organization
        // LOG.info("Assign roles to Doctor and Nurse")
        assert(healthMgmtFacility.assignRole(hospital.identity, doctor.identity, "Doctor"))
        assert(healthMgmtFacility.assignRole(hospital.identity, nurse.identity, "Nurse"))

        // create empty record for alice
        // LOG.info("Create record for Alice")
        assert(healthMgmtFacility.createEmptyRecord(alice.identity))

        // ensure alice can read her record, i.e. no exception
        // LOG.info("Ensure Alice can read her record")
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

        // ensure doctor cannot read-partially alice's record yet
        assertThrows<HealthcareException> {
            healthMgmtFacility.accessRecord(
                doctor.identity,
                alice.identity,
                LuceRight(PatientRecord.RECORD_RIGHT_ID_READ_PARTIAL),
                UsageListener()
            )
        }

        // deploy policy that allows doctor and nurse to access the record
        // ensure alice can read her record, i.e. no exception
        // LOG.info("Deploy Usage Policy as Alice for her record to allow role-based usage for Doctor and Nurse")
        val listenerAlicePolicy = UsageListener()
        val handleAlicePolicy = healthMgmtFacility.accessRecord(
            alice.identity,
            alice.identity,
            LuceRight(PatientRecord.RECORD_RIGHT_ID_CREATE_POLICY),
            listenerAlicePolicy
        )
        listenerAlicePolicy.setHandle(handleAlicePolicy)

        // create and deploy policy
        val policyAlice = LuceLang.Policy(
            id = "policyAlice1",
            issuer = alice.identity,
            contexts = setOf(LuceLang.PolicyContext.ObjectId(handleAlicePolicy.recordId()!!)),
            rights = setOf(PatientRecord.RECORD_RIGHT_ID_READ_PARTIAL, PatientRecord.RECORD_RIGHT_ID_APPEND),
            preAccess = LuceLang.PreAccess(
                predicates = listOf(
                    LuceLang.Predicate.Rbac(
                        "attr_pip",
                        "attr_pip"
                    ),
                    LuceLang.Predicate.UsageNotification(
                        "default_notification_service",
                        "time_pip"
                    )
                )
            ),
            ongoingAccess = LuceLang.OnAccess(
                triggers = listOf(LuceLang.Trigger.PeriodicTrigger(5000L)),
                predicates = listOf(LuceLang.Predicate.Rbac("attr_pip", "attr_pip"))
            ),
            postAccess = LuceLang.PostAccess(
                predicates = listOf()
            ),
            postRevocation = LuceLang.PostRevocation(
                predicates = listOf()
            ),
        )

        val signatureAlice = "signature" // TODO sign with private key
        val policyWrapperAlice = LuceLang.PolicyWrapper(policyAlice, signatureAlice)
        healthMgmtFacility.deployPolicy(Json.encodeToString(policyWrapperAlice))

        // deploy a second that allows local deletion for everyone
        val policyAlice2 = LuceLang.Policy(
            id = "policyAlice2",
            issuer = alice.identity,
            contexts = setOf(LuceLang.PolicyContext.ObjectId(handleAlicePolicy.recordId()!!)),
            rights = setOf(PatientRecord.RECORD_RIGHT_ID_DELETE_LOCAL),
            preAccess = LuceLang.PreAccess(
                predicates = listOf(
                    LuceLang.Predicate.UsageNotification(
                        "default_notification_service",
                        "time_pip"
                    )
                )
            ),
            ongoingAccess = LuceLang.OnAccess(
                triggers = listOf(),
                predicates = listOf()
            ),
            postAccess = LuceLang.PostAccess(
                predicates = listOf()
            ),
            postRevocation = LuceLang.PostRevocation(
                predicates = listOf()
            ),
        )
        val signatureAlice2 = "signature" // TODO sign with private key
        val policyWrapperAlice2 = LuceLang.PolicyWrapper(policyAlice2, signatureAlice2)
        healthMgmtFacility.deployPolicy(Json.encodeToString(policyWrapperAlice2))

        // drop handle
        handleAlicePolicy.drop(false)

        // ensure doctor can append
        // LOG.info("Ensure Doctor can now append Alice's record")
        val listenerDoctorAppend = UsageListener()
        val handleDoctorAppend = healthMgmtFacility.accessRecord(
            doctor.identity,
            alice.identity,
            LuceRight(PatientRecord.RECORD_RIGHT_ID_APPEND),
            listenerDoctorAppend
        )
        listenerDoctorAppend.setHandle(handleDoctorAppend)
        handleDoctorAppend.drop(false)

        // ensure nurse can partially read
        val listenerNurseReadPartial = UsageListener()
        val handleNurseReadPartial = healthMgmtFacility.accessRecord(
            nurse.identity,
            alice.identity,
            LuceRight(PatientRecord.RECORD_RIGHT_ID_READ_PARTIAL),
            listenerNurseReadPartial
        )
        listenerNurseReadPartial.setHandle(handleNurseReadPartial)
        handleNurseReadPartial.drop(false)

        // ensure doctor cannot fully read
        assertThrows<HealthcareException> {
            healthMgmtFacility.accessRecord(
                doctor.identity,
                alice.identity,
                LuceRight(PatientRecord.RECORD_RIGHT_ID_READ),
                UsageListener()
            )
        }

        // delegate rights to Bob
        val listenerAliceDelegate = UsageListener()
        val handleAliceDelegate = healthMgmtFacility.accessRecord(
            alice.identity,
            alice.identity,
            LuceRight(PatientRecord.RECORD_RIGHT_ID_DELEGATE_RIGHT),
            listenerAliceDelegate
        )
        listenerAliceDelegate.setHandle(handleAliceDelegate)
        // delegate rights to Bob
        assert(handleAliceDelegate.delegateRight(bob.identity, LuceRight(PatientRecord.RECORD_RIGHT_ID_CREATE_POLICY)))
        assert(handleAliceDelegate.delegateRight(bob.identity, LuceRight(PatientRecord.RECORD_RIGHT_ID_DELEGATE_RIGHT)))
        assert(handleAliceDelegate.delegateRight(bob.identity, LuceRight(PatientRecord.RECORD_RIGHT_ID_READ)))
        assert(handleAliceDelegate.delegateRight(bob.identity, LuceRight(PatientRecord.RECORD_RIGHT_ID_READ_PARTIAL)))
        assert(handleAliceDelegate.delegateRight(bob.identity, LuceRight(PatientRecord.RECORD_RIGHT_ID_APPEND)))
        assert(handleAliceDelegate.delegateRight(bob.identity, LuceRight(PatientRecord.RECORD_RIGHT_ID_DELETE_LOCAL)))
        handleAliceDelegate.drop(false)

        // ensure Bob can deploy policy that allows the doctor to read the record for 10 seconds under the condition
        //   that local copies are deleted afterwards
        val listenerBobPolicy = UsageListener()
        val handleBobPolicy = healthMgmtFacility.accessRecord(
            bob.identity,
            alice.identity,
            LuceRight(PatientRecord.RECORD_RIGHT_ID_CREATE_POLICY),
            listenerBobPolicy
        )
        listenerBobPolicy.setHandle(handleBobPolicy)
        // create and deploy policy
        val now = LocalDateTime.now()
        val policyBob = LuceLang.Policy(
            id = "policyBob1",
            issuer = bob.identity,
            contexts = setOf(LuceLang.PolicyContext.ObjectId(handleBobPolicy.recordId()!!)),
            rights = setOf(PatientRecord.RECORD_RIGHT_ID_READ),
            preAccess = LuceLang.PreAccess(
                predicates = listOf(
                    LuceLang.Predicate.Rbac(
                        "attr_pip",
                        "attr_pip"
                    ),
                    LuceLang.Predicate.DurationRestriction(
                        "time_pip",
                        now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        1L,
                        ZoneId.systemDefault().id
                    ),
                    LuceLang.Predicate.UsageNotification(
                        "default_notification_service",
                        "time_pip"
                    )
                )
            ),
            ongoingAccess = LuceLang.OnAccess(
                triggers = listOf(LuceLang.Trigger.PeriodicTrigger(500L)),
                predicates = listOf(
                    LuceLang.Predicate.Rbac("attr_pip", "attr_pip"),
                    LuceLang.Predicate.DurationRestriction(
                        "time_pip",
                        now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        1L,
                        ZoneId.systemDefault().id
                    ),
                )
            ),
            postAccess = LuceLang.PostAccess(
                predicates = listOf(
                    LuceLang.Predicate.Dependency("delete_local")
                )
            ),
            postRevocation = LuceLang.PostRevocation(
                predicates = listOf(
                    LuceLang.Predicate.Dependency("delete_local")
                )
            ),
        )

        val signatureBob = "signature" // TODO sign with private key
        val policyWrapperBob = LuceLang.PolicyWrapper(policyBob, signatureBob)
        healthMgmtFacility.deployPolicy(Json.encodeToString(policyWrapperBob))
        handleBobPolicy.drop(false)

        // ensure doctor can fully read the record
        // problem: 'read' is not in rbac rpa -> assign emergency role to doctor
        healthMgmtFacility.assignRole(hospital.identity, doctor.identity, "Emergency")
        // provide a delete_local dependency procedure, that deletes the
        val listenerDoctorRead = UsageListener()
        var deleted = false
        listenerDoctorRead.setDeletionProcedure {
            val handleDoctorDelete = healthMgmtFacility.accessRecord(
                doctor.identity,
                alice.identity,
                LuceRight(PatientRecord.RECORD_RIGHT_ID_DELETE_LOCAL),
                UsageListener()
            )
            // allowed to delete the local copies
            deleted = true
            handleDoctorDelete.drop(false)
            true
        }
        val handleDoctorRead = healthMgmtFacility.accessRecord(
            doctor.identity,
            alice.identity,
            LuceRight(PatientRecord.RECORD_RIGHT_ID_READ),
            listenerDoctorRead
        )
        listenerDoctorRead.setHandle(handleDoctorRead)
        // wait until revocation
        Thread.sleep(1500)
        assert(handleDoctorRead.isRevoked())

        // ensure the local copy is deleted after the usage
        assert(deleted)

        // ensure the doctor cannot read the full record anymore, since usage interval is over
        assertThrows<HealthcareException> {
            healthMgmtFacility.accessRecord(
                doctor.identity,
                alice.identity,
                LuceRight(PatientRecord.RECORD_RIGHT_ID_READ),
                UsageListener()
            )
        }
    }
}
package core.control_flow_model.messages

import core.admin.LuceObject
import core.admin.LuceRight
import core.admin.LuceSubject
import core.control_flow_model.components.PolicyEnforcementPoint

/**
 * Decision Request, used for requesting a new decision request for a usage (S,O,R)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class DecisionRequest<Sid, Oid> (
    val luceSubject: LuceSubject<Sid>,
    val luceObject: LuceObject<Sid, Oid>,
    val luceRight: LuceRight,
    val listener: PolicyEnforcementPoint,
)
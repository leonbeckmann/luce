package core.control_flow_model.messages

import core.admin.LuceObject
import core.admin.LuceRight
import core.admin.LuceSubject

/**
 * Decision Request
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class DecisionRequest<Sid, Oid> (
    val luceSubject: LuceSubject<Sid>,
    val luceObject: LuceObject<Sid, Oid>,
    val luceRight: LuceRight
)
package core.control_flow_model.messages

import core.LuceObject
import core.LuceRight
import core.LuceSubject

/**
 * Decision Request
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class DecisionRequest<Sid, Oid>(
    val subject: LuceSubject<Sid>,
    val obj: LuceObject<Sid, Oid>,
    val right: LuceRight
)
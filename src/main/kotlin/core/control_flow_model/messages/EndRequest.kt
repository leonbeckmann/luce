package core.control_flow_model.messages

import core.admin.LuceObject
import core.admin.LuceRight
import core.admin.LuceSubject

/**
 * End Request
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class EndRequest<Sid, Oid> (
    val luceSubject: LuceSubject<Sid>,
    val luceObject: LuceObject<Sid, Oid>,
    val luceRight: LuceRight
)
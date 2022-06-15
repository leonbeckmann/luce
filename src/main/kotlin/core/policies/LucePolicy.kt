package core.policies

import it.unibo.tuprolog.core.Atom
import it.unibo.tuprolog.core.Struct
import it.unibo.tuprolog.core.Term
import it.unibo.tuprolog.dsl.prolog
import kotlin.math.min

/**
 * Low-level LUCE policy, as defined in LUCE's enforcement model (see Section 6.2)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class LucePolicy(
    val preAccess: Struct,          // CR1 + UR1 + DR1
    val postPermit: Struct,         // UR2
    val ongoingAccess: Struct,      // CR2 + UR3 + UR4 + DR2
    val ongoingPeriod: Long?,        // period (in ms) = reevaluation timer interval
    val postAccessRevoked: Struct,  // UR5 + DR3
    val postAccessEnded: Struct     // UR6 + DR4
) {

    /**
     * A function to merge together two low-level policies according to "Deny-overrides" from "Policy Combining" in 6.3.2
     *
     * @return A fresh policy, combining this and the given policy
     */
    fun mergeByConjunction(policy: LucePolicy) : LucePolicy {
        // TODO Future Work: Specify how to combine multiple triggers
        val period = if (ongoingPeriod != null && policy.ongoingPeriod != null) {
            min(ongoingPeriod, policy.ongoingPeriod)
        } else ongoingPeriod ?: policy.ongoingPeriod

        return LucePolicy(
            preAccess = prolog { preAccess and policy.preAccess },
            postPermit = prolog { postPermit and policy.postPermit },
            ongoingAccess = prolog { ongoingAccess and policy.ongoingAccess },
            ongoingPeriod = period,
            postAccessEnded = prolog { postAccessEnded and policy.postAccessEnded },
            postAccessRevoked = prolog { postAccessRevoked and policy.postAccessRevoked }
        )
    }

    /**
     * Returns a fresh policy for which $OBJECT, $SUBJECT and $RIGHT are replaced by their specific identities
     */
    fun replaceVariables(
        sId: String,
        oId: String,
        rId: String
    ) : LucePolicy {
        return LucePolicy(
            replaceVarsInTerm(this.preAccess, sId, oId, rId).castToStruct(),
            replaceVarsInTerm(this.postPermit, sId, oId, rId).castToStruct(),
            replaceVarsInTerm(this.ongoingAccess, sId, oId, rId).castToStruct(),
            this.ongoingPeriod,
            replaceVarsInTerm(this.postAccessRevoked, sId, oId, rId).castToStruct(),
            replaceVarsInTerm(this.postAccessEnded, sId, oId, rId).castToStruct(),
        )
    }

    companion object {
        private fun replaceVarsInTerm(
            t: Term,
            sId: String,
            oId: String,
            rId: String
        ) : Term {
            return when (t) {
                is Atom -> {
                    val value = t.value
                        .replace("\$SUBJECT", sId)
                        .replace("\$OBJECT", oId)
                        .replace("\$RIGHT", rId)
                    Atom.of(value)
                }
                is Struct -> {
                    Struct.of(t.functor, t.args.map { replaceVarsInTerm(it, sId, oId, rId) })
                }
                else -> t
            }
        }
    }

}
package core.policies

import it.unibo.tuprolog.core.Struct

/**
 * Low-level LUCE policy
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
     * A function to merge together two low-level policies according to "Policy Combining" in 6.3.3
     *
     * @return A fresh policy, combining this and the given policy
     */
    fun merge(policy: LucePolicy) : LucePolicy {
        TODO("Not yet implemented")
    }

}
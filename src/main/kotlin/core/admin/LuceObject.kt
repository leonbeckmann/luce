package core.admin

/**
 * LUCE Object, as defined in LUCE's administrative model (see Section 5.2.2)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
abstract class LuceObject<Sid, Oid>(
    val identity: Oid,
    val owner: Sid,
    val type: String,
    ownerRights: Set<LuceRight>
) {

    // rights mapping: who has which right on this object
    val rights = mutableMapOf<String, MutableSet<LuceRight>>()

    init {
        // the object owner has a set of initial rights
        rights[owner.toString()] = ownerRights.toMutableSet()
    }

}
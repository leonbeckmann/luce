package core

/**
 * LUCE Object
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
abstract class LuceObject<Sid, Oid>(
    val identity: Oid,
    val owner: Sid,
    val type: String,
    ownerRights: Set<LuceRight>
) {
    protected val rights = mutableMapOf<Sid, MutableSet<LuceRight>>()

    init {
        rights[owner] = ownerRights.toMutableSet()
    }

}
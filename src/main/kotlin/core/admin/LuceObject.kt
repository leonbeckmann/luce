package core.admin

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

    val rights = mutableMapOf<String, MutableSet<LuceRight>>()

    init {
        rights[owner.toString()] = ownerRights.toMutableSet()
    }

}
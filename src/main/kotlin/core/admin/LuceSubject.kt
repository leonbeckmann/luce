package core.admin

/**
 * LUCE Subject
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
abstract class LuceSubject<Sid>(
    identity: Sid,
    owner: Sid,
    ownerRights: Set<LuceRight>
) : LuceObject<Sid, Sid>(identity, owner, "subject", ownerRights)
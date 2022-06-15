package core.admin

/**
 * LUCE Subject, as defined in LUCE's administrative model (see Section 5.2.2)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
abstract class LuceSubject<Sid>(
    identity: Sid,
    owner: Sid,
    ownerRights: Set<LuceRight>
) : LuceObject<Sid, Sid>(identity, owner, "subject", ownerRights)
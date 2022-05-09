package policy_language

import core.policies.LucePolicy

/**
 * Interface for high-level policy language implementations
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyLanguage<T> {

    /**
     * Deserialize serialized policy to inner policy object
     */
    fun deserialize(serialized: String) : T

    /**
     * Translate policy object to LucePolicy
     */
    fun translate(obj: T) : LucePolicy

    /**
     * Return the id of the policy language
     */
    fun id() : String

}
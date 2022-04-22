package core.policies

interface PolicyLanguage<T> {

    fun deserialize(serialized: String) : T
    fun translate(obj: T) : LucePolicy
    fun id() : String

}
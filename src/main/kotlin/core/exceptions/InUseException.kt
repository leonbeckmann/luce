package core.exceptions

/**
 * InUse Exception to catch new requests to ongoing usages
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class InUseException : LuceException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
package core.exceptions

/**
 * LUCE Exception
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class LuceException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
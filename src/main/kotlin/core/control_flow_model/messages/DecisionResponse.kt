package core.control_flow_model.messages

/**
 * Decision Response, returned by the PDP to the decision requester
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
enum class DecisionResponse {

    PERMITTED,
    DENIED,
    IN_USE;

    fun isPermitted() : Boolean {
        return this == PERMITTED
    }

    fun isDenied() : Boolean {
        return this == DENIED
    }

    fun isInUse() : Boolean {
        return this == IN_USE
    }

}
package core.control_flow_model.messages

/**
 * Decision Response
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
enum class DecisionResponse {

    PERMITTED,
    DENIED;

    fun isPermitted() : Boolean {
        return this == PERMITTED
    }

    fun isDenied() : Boolean {
        return this == DENIED
    }

}
package core.control_flow_model.components

/**
 * LUCE PIP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyInformationPoint {

    fun queryInformation(identifier: Any) : Any?
    fun updateInformation(identifier: Any, description: String, value: Any?) : Boolean

}
package core.control_flow_model.components

/**
 * LUCE PIP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyInformationPoint {

    fun queryInformation(identifier: Any) : Any?
    fun updateInformationByValue(identifier: Any, newValue: Any?) : Boolean
    fun updateInformation(identifier: Any, description: String) : Boolean

}
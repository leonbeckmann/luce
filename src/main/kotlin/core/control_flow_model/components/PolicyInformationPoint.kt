package core.control_flow_model.components

/**
 * LUCE PIP
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyInformationPoint {

    fun queryInformation(informationId: String) : Any?
    fun updateInformation(informationId: String, newValue: Any?)

}
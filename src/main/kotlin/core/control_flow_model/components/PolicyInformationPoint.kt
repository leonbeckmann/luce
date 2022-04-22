package core.control_flow_model.components

/**
 * Policy Information Point (PIP)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface PolicyInformationPoint {

    fun <T> query(urn: String) : T?

}
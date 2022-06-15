package core.control_flow_model.messages

import it.unibo.tuprolog.solve.Solution

/**
 * EndResponse, returned by the PDP to the endaccess requester
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class EndResponse(val solution: Solution)
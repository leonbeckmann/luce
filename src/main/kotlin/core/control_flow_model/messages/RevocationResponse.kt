package core.control_flow_model.messages

import it.unibo.tuprolog.solve.Solution

/**
 * RevocationResponse, passed by the PDp to the PEP listener
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class RevocationResponse(val solution: Solution)
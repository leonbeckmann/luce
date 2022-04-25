package core.control_flow_model.components

import org.slf4j.LoggerFactory
import java.util.Timer
import java.util.TimerTask

/**
 * LUCE Reevaluation Timer
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class ReevaluationTimer(
    private val delay: Long,
    private val period: Long,
    private val sessionId: String,
) {

    private val timer = Timer()

    fun schedule() {
        if (LOG.isTraceEnabled) {
            LOG.trace("Schedule new timer=$this after delay=${delay}ms with period=${period}ms")
        }
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                PolicyDecisionPoint.triggerPeriodic(sessionId)
            }
        }, delay, period)
    }

    fun cancel() {
        if (LOG.isTraceEnabled) {
            LOG.trace("Cancel timer=$this")
        }
        timer.cancel()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ReevaluationTimer::class.java)
    }

}
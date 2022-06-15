package core.control_flow_model.components

import org.slf4j.LoggerFactory
import java.util.Timer
import java.util.TimerTask

/**
 * LUCE Reevaluation Timer,as defined in LUCE's control flow model (see Section 6.1.1)
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class ReevaluationTimer(
    private val delay: Long,
    private val period: Long,
    private val sessionId: String,
) {

    private val timer = Timer()

    /**
     * Run the timer instance and schedule periodic jobs that trigger the PDP for the given session
     */
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

    /**
     * Cancel the timer, including the current and all future jobs
     */
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
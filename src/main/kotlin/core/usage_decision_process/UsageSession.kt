package core.usage_decision_process

import core.control_flow_model.components.PolicyEnforcementPoint
import core.control_flow_model.components.ReevaluationTimer
import core.policies.LucePolicy
import java.util.concurrent.locks.ReentrantLock

/**
 * Usage Decision Process, including its FSM
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class UsageSession(val id: String) {

    /**
     * Usage Session States
     */
    sealed class State(val id: String)

    object Initial : State("initial")

    object Requesting : State("requesting")

    object Denied : State("denied")

    data class Accessing(
        val policy: LucePolicy,
        val listener: PolicyEnforcementPoint,
        var reevaluationTimer: ReevaluationTimer?
    ) : State("accessing") {
        companion object {
            const val id = "accessing"
        }
    }

    object Revoked : State("revoked")

    object End : State("end")

    object Error : State("error")

    /**
     * FSM event to trigger transitions
     */
    sealed class Event(val id: String)

    object TryAccess : Event("try_access")

    object DenyAccess : Event("deny_access")

    data class PermitAccess(
        val policy: LucePolicy,
        val listener: PolicyEnforcementPoint,
        var reevaluationTimer: ReevaluationTimer?
    ) : Event("permit_access") {
        companion object {
            const val id = "permit_access"
        }
    }

    object RevokeAccess : Event("revoke_access")

    object EndAccess : Event("end_access")

    /**
     * FSM transition, triggered by fsm event
     */
    class Transition(private val eventHandler: (Event) -> State) {
        fun doTransition(e: Event): State {
            return eventHandler(e)
        }
    }

    /**
     * Current session state
     */
    var state: State = Initial
        private set

    fun reset() {
        this.state = Initial
    }

    fun cancelTimer() {
        if (state is Accessing) {
            (state as Accessing).reevaluationTimer?.cancel()
            (state as Accessing).reevaluationTimer = null
        }
    }

    /**
     * Synchronization for fsm
     */
    val lock = ReentrantLock()

    /**
     * Registered transitions
     */
    private data class TransitionKey(val state: String, val event: String)
    private val transitions = HashMap<TransitionKey, Transition>()
    private val defaultTransition: Transition = Transition {
        Error
    }

    init {
        // (Initial, TryAccess) -> Requesting
        transitions[TransitionKey(Initial.id, TryAccess.id)] = Transition {
            Requesting
        }

        // (Requesting, DenyAccess) -> Denied
        transitions[TransitionKey(Requesting.id, DenyAccess.id)] = Transition {
            Denied
        }

        // (Requesting, PermitAccess) -> Accessing
        transitions[TransitionKey(Requesting.id, PermitAccess.id)] = Transition {
            if (it is PermitAccess) {
                Accessing(it.policy, it.listener, it.reevaluationTimer)
            } else {
                Error
            }
        }

        // (Accessing, RevokeAccess) -> Revoked
        transitions[TransitionKey(Accessing.id, RevokeAccess.id)] = Transition {
            Revoked
        }

        // (Accessing, EndAccess) -> End
        transitions[TransitionKey(Accessing.id, EndAccess.id)] = Transition {
            End
        }
    }

    fun feedEvent(e: Event) {
        val t = transitions.getOrDefault(TransitionKey(state.id, e.id), defaultTransition)
        this.state = t.doTransition(e)
    }

    fun lock() {
        lock.lock()
    }

    fun unlock() {
        lock.unlock()
    }
}
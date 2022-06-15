package core.usage_decision_process

import core.control_flow_model.components.PolicyEnforcementPoint
import core.control_flow_model.components.ReevaluationTimer
import core.policies.LucePolicy
import java.util.concurrent.locks.ReentrantLock

/**
 * Usage Decision Process, including its FSM (see Section 5.2.1)
 *
 * The FSM only contains transitions that change the state.
 * All other transitions are handled implicitly.
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class UsageSession(val id: String) {

    /**
     * Usage Session States
     */
    sealed class State(val id: String) {
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
    }

    /**
     * FSM event to trigger transitions
     */
    sealed class Event(val id: String) {
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
    }

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
    var state: State = State.Initial
        private set

    /**
     * Reset the session to initial state
     */
    fun reset() {
        this.state = State.Initial
    }

    /**
     * Cancel the re-evaluation timer
     */
    fun cancelTimer() {
        if (state is State.Accessing) {
            // only accessing state has reevaluation timer
            (state as State.Accessing).reevaluationTimer?.cancel()
            (state as State.Accessing).reevaluationTimer = null
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

    // default transition into Error state, called when no other transition matches
    private val defaultTransition: Transition = Transition {
        State.Error
    }

    init {
        // (Initial, TryAccess) -> Requesting
        transitions[TransitionKey(State.Initial.id, Event.TryAccess.id)] = Transition {
            State.Requesting
        }

        // (Requesting, DenyAccess) -> Denied
        transitions[TransitionKey(State.Requesting.id, Event.DenyAccess.id)] = Transition {
            State.Denied
        }

        // (Requesting, PermitAccess) -> Accessing
        transitions[TransitionKey(State.Requesting.id, Event.PermitAccess.id)] = Transition {
            if (it is Event.PermitAccess) {
                State.Accessing(it.policy, it.listener, it.reevaluationTimer)
            } else {
                State.Error
            }
        }

        // (Accessing, RevokeAccess) -> Revoked
        transitions[TransitionKey(State.Accessing.id, Event.RevokeAccess.id)] = Transition {
            State.Revoked
        }

        // (Accessing, EndAccess) -> End
        transitions[TransitionKey(State.Accessing.id, Event.EndAccess.id)] = Transition {
            State.End
        }
    }

    /**
     * Feed a event into the FSM, trigger the corresponding transition for (state, event)
     */
    fun feedEvent(e: Event) {
        val t = transitions.getOrDefault(TransitionKey(state.id, e.id), defaultTransition)
        this.state = t.doTransition(e)
    }

    /**
     * Lock the session for mutual access
     */
    fun lock() {
        lock.lock()
    }

    /**
     * Unlock the session
     */
    fun unlock() {
        lock.unlock()
    }
}
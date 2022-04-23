package core.usage_decision_process


import java.util.concurrent.locks.ReentrantLock

/**
 * Usage Decision Process, including its FSM
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
class UsageSession {

    /**
     * Usage Session States
     */
    enum class State {
        Initial,
        Requesting,
        Denied,
        Accessing,
        Revoked,
        End,
        Error
    }

    /**
     * FSM event to trigger transitions
     */
    enum class Event {
        TryAccess,
        PreUpdate,
        PreDeps,
        DenyAccess,
        PermitAccess,
        OnUpdate,
        OnDeps,
        RevokeAccess,
        EndAccess,
        PostUpdate,
        PostDeps
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
     * Synchronization for fsm
     */
    private val lock = ReentrantLock()

    /**
     * Registered transitions
     */
    private data class TransitionKey(val state: State, val e: Event)
    private val transitions = HashMap<TransitionKey, Transition>()
    private val defaultTransition: Transition = Transition {
        State.Error
    }

    init {
        // (Initial, TryAccess) -> Requesting
        transitions[TransitionKey(State.Initial, Event.TryAccess)] = Transition {
            State.Requesting
        }

        // (Requesting, PreUpdate) -> Requesting
        transitions[TransitionKey(State.Requesting, Event.PreUpdate)] = Transition {
            State.Requesting
        }

        // (Requesting, PreDeps) -> Requesting
        transitions[TransitionKey(State.Requesting, Event.PreDeps)] = Transition {
            State.Requesting
        }

        // (Requesting, DenyAccess) -> Denied
        transitions[TransitionKey(State.Requesting, Event.DenyAccess)] = Transition {
            State.Denied
        }

        // (Requesting, PermitAccess) -> Accessing
        transitions[TransitionKey(State.Requesting, Event.PermitAccess)] = Transition {
            State.Accessing
        }

        // (Denied, PreUpdate) -> Denied
        transitions[TransitionKey(State.Denied, Event.PreUpdate)] = Transition {
            State.Denied
        }

        // (Accessing, OnUpdate) -> Accessing
        transitions[TransitionKey(State.Accessing, Event.OnUpdate)] = Transition {
            State.Accessing
        }

        // (Accessing, OnDeps) -> Accessing
        transitions[TransitionKey(State.Accessing, Event.OnDeps)] = Transition {
            State.Accessing
        }

        // (Accessing, RevokeAccess) -> Revoked
        transitions[TransitionKey(State.Accessing, Event.RevokeAccess)] = Transition {
            State.Revoked
        }

        // (Accessing, EndAccess) -> End
        transitions[TransitionKey(State.Accessing, Event.EndAccess)] = Transition {
            State.End
        }

        // (Revoked, PostUpdate) -> Revoked
        transitions[TransitionKey(State.Revoked, Event.PostUpdate)] = Transition {
            State.Revoked
        }

        // (Revoked, PostDeps) -> Revoked
        transitions[TransitionKey(State.Revoked, Event.PostDeps)] = Transition {
            State.Revoked
        }

        // (End, PostUpdate) -> PostUpdate
        transitions[TransitionKey(State.End, Event.PostUpdate)] = Transition {
            State.End
        }

        // (End, PostDeps) -> End
        transitions[TransitionKey(State.End, Event.PostDeps)] = Transition {
            State.End
        }
    }

    fun feedEvent(e: Event) {
        val t = transitions.getOrDefault(TransitionKey(state, e), defaultTransition)
        this.state = t.doTransition(e)
    }

    fun lock() {
        lock.lock()
    }

    fun unlock() {
        lock.unlock()
    }
}
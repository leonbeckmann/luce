package core.notification

/**
 * Monitor client interface and registry, reachable from Prolog engine to send usage notifications
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface MonitorClient {
    fun notify(notification: String) : Boolean

    companion object {
        val registry = mutableMapOf<String, MonitorClient>()

        fun register(id: String, monitor: MonitorClient) {
            registry[id] = monitor
        }
    }
}
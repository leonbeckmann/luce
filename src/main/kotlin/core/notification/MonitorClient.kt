package core.notification

/**
 * Monitor client interface and registry, reachable from Prolog engine to send usage notifications
 *
 * @author Leon Beckmann <leon.beckmann@tum.de>
 */
interface MonitorClient {

    /**
     * Send a notification via this monitor client to its specified endpoint
     */
    fun notify(notification: String) : Boolean

    companion object {

        // a registry to register monitor clients, identified by ID string
        val registry = mutableMapOf<String, MonitorClient>()

        /**
         * Register a new monitor client
         */
        fun register(id: String, monitor: MonitorClient) {
            registry[id] = monitor
        }
    }
}
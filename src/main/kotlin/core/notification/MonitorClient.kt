package core.notification

interface MonitorClient {
    fun notify(notification: String) : Boolean

    companion object {
        val registry = mutableMapOf<String, MonitorClient>()

        fun register(id: String, monitor: MonitorClient) {
            registry[id] = monitor
        }
    }
}
package com.group7

open class Sink<InputT>(label: String, incoming: List<InputChannel<InputT>>) :
    Node<Nothing, InputT, Nothing>(label, incoming, emptyList()) {
    // Could be memory intensive, can keep a map of objects to counts
    var load: MutableList<InputT> = mutableListOf()

    override fun onArrive(simulator: Simulator, obj: InputT) {
        load.add(obj)
    }

    override fun onEmit(simulator: Simulator) {
        error("Can't emit from a sink! What are you doing?!")
    }

    override fun reportMetrics(): Metrics {
        return Metrics(null, load.size)
    }
}

package com.group7

open class Sink<InputT>(label: String, incoming: List<InputChannel<InputT>>) :
    Node<Nothing, InputT, Nothing>(label, incoming, emptyList()) {
    // Could be memory intensive, can keep a map of objects to counts
    private val load: MutableList<InputT> = mutableListOf()

    context(_: Simulator)
    override fun onArrive(obj: InputT) {
        load.add(obj)
    }

    context(_: Simulator)
    override fun onEmit() {
        error("Can't emit from a sink! What are you doing?!")
    }

    override fun reportMetrics(): Metrics {
        return Metrics(null, load.size)
    }
}

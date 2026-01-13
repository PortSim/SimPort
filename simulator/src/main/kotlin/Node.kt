package com.group7

abstract class Node<in EventT, InputT, OutputT>(
    val label: String,
    val incoming: List<InputChannel<InputT>>,
    val outgoing: List<OutputChannel<OutputT>>,
) {
    init {
        incoming.forEach { (it as ChannelImpl).setReceivingNode(this) }
    }

    // What to do when something arrives,
    // call onEvent with some customisation? (instantaneous)
    // Never fail
    abstract fun onArrive(simulator: Simulator, obj: InputT)

    // Processing of a thing, essentially a fancy delay (takes take)
    open fun onEvent(simulator: Simulator, event: EventT) {}

    // What to do when something is ready to leave,
    // Tells the simulator what node to emit to? (instantaneous)
    // Handles failures to emit
    abstract fun onEmit(simulator: Simulator)

    open fun reportMetrics(): Metrics = Metrics()
}

data class Metrics(
    val currentLoad: Float? = null,
)
package com.group7

import kotlin.time.Duration

abstract class Node<in EventT, InputT, OutputT>(
    val label: String,
    val incoming: List<InputChannel<InputT>>,
    val outgoing: List<OutputChannel<OutputT>>,
) {
    init {
        outgoing.forEach { (it as ChannelImpl).setUpstreamNode(this) }
        incoming.forEach { (it as ChannelImpl).setDownstreamNode(this) }
    }

    // What to do when something arrives,
    // call onEvent with some customisation? (instantaneous)
    // Never fail
    context(_: Simulator)
    abstract fun onArrive(obj: InputT)

    // Processing of a thing, essentially a fancy delay (takes take)
    context(_: Simulator)
    open fun onEvent(event: EventT) {}

    // What to do when something is ready to leave,
    // Tells the simulator what node to emit to? (instantaneous)
    // Handles failures to emit
    context(_: Simulator)
    abstract fun onEmit()

    open fun reportMetrics() = Metrics()

    context(sim: Simulator)
    protected fun scheduleEvent(delay: Duration, event: EventT) {
        (sim as SimulatorImpl).scheduleEvent(this, delay, event)
    }

    context(sim: Simulator)
    protected fun scheduleEmit(delay: Duration) {
        (sim as SimulatorImpl).scheduleEmit(this, delay)
    }

    context(sim: Simulator)
    protected fun emitWhenOpen(vararg waitingFor: OutputChannel<OutputT>) {
        (sim as SimulatorImpl).emitWhenOpen(this, *waitingFor)
    }

    override fun toString() = label
}

data class Metrics(val percentageFull: Float? = null, val occupants: Int? = null)

abstract class SourceNode<in EventT, OutputT>(label: String, outgoing: List<OutputChannel<OutputT>>) :
    Node<EventT, Nothing, OutputT>(label, emptyList(), outgoing) {

    context(_: Simulator)
    abstract fun onStart()
}

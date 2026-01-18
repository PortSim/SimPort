package com.group7

import kotlin.time.Duration

abstract class Node(val label: String, private val outgoing: List<OutputChannel<*>>) {
    private val incoming = mutableListOf<InputChannel<*>>()

    init {
        outgoing.forEach { (it as ChannelImpl).setUpstreamNode(this) }
    }

    protected fun <T> InputChannel<T>.onReceive(
        callback:
            context(Simulator)
            (T) -> Unit
    ) {
        incoming.add(this)
        (this as ChannelImpl).setDownstreamNode(this@Node, callback)
    }

    open fun reportMetrics() = Metrics()

    override fun toString() = label

    protected companion object {
        @JvmStatic
        context(_: Simulator)
        protected fun schedule(callback: () -> Unit) {
            scheduleDelayed(Duration.ZERO, callback)
        }

        @JvmStatic
        context(sim: Simulator)
        protected fun scheduleDelayed(delay: Duration, callback: () -> Unit) {
            (sim as SimulatorImpl).scheduleDelayed(delay, callback)
        }
    }
}

data class Metrics(val occupants: Int? = null)

abstract class SourceNode(label: String, outgoing: List<OutputChannel<*>>) : Node(label, outgoing) {

    context(_: Simulator)
    abstract fun onStart()
}

package com.group7

import kotlin.time.Duration

abstract class Node(val label: String, vararg outgoing: OutputChannel<*>) {
    private val incoming = mutableListOf<InputChannel<*>>()
    private val outgoing = outgoing.toList()

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
        context(sim: Simulator)
        protected fun scheduleDelayed(delay: Duration, callback: () -> Unit) {
            (sim as SimulatorImpl).scheduleDelayed(delay, callback)
        }

        @JvmStatic
        context(sim: Simulator)
        protected fun scheduleWhenOpened(vararg waitingFor: OutputChannel<*>, callback: () -> Unit) {
            (sim as SimulatorImpl).scheduleWhenOpened(waitingFor, callback)
        }
    }
}

data class Metrics(val percentageFull: Float? = null, val occupants: Int? = null)

abstract class SourceNode(label: String, vararg outgoing: OutputChannel<*>) : Node(label, *outgoing) {

    context(_: Simulator)
    abstract fun onStart()
}

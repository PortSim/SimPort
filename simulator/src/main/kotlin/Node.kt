package com.group7

import kotlin.time.Duration

abstract class Node(label: String, final override val outgoing: List<OutputChannel<*>>) : NodeGroup(label) {
    private val _incoming = mutableListOf<InputChannel<*>>()
    val incoming: List<InputChannel<*>>
        get() = _incoming

    init {
        outgoing.forEach { it.asImpl().setUpstreamNode(this) }
    }

    protected fun <T> InputChannel<T>.onReceive(
        callback:
            context(Simulator)
            (T) -> Unit
    ) {
        _incoming.add(this)
        this.asImpl().setDownstreamNode(this@Node, callback)
    }

    context(_: Simulator)
    open fun onStart() {}

    open fun reportMetrics() = Metrics()

    protected companion object {
        @JvmStatic
        context(_: Simulator)
        protected fun schedule(callback: () -> Unit) {
            scheduleDelayed(Duration.ZERO, callback)
        }

        @JvmStatic
        context(sim: Simulator)
        protected fun scheduleDelayed(delay: Duration, callback: () -> Unit) {
            sim.asImpl().scheduleDelayed(delay, callback)
        }
    }
}

data class Metrics(val occupants: Int? = null)

abstract class SourceNode(label: String, outgoing: List<OutputChannel<*>>) : Node(label, outgoing) {

    context(_: Simulator)
    abstract override fun onStart()
}

package com.group7

import com.group7.channels.PushInputChannel
import com.group7.channels.PushOutputChannel
import com.group7.channels.asImpl
import kotlin.time.Duration

abstract class Node(label: String, final override val outgoing: List<PushOutputChannel<*>>) : NodeGroup(label) {
    private val _incoming = mutableListOf<PushInputChannel<*>>()
    val incoming: List<PushInputChannel<*>>
        get() = _incoming

    init {
        outgoing.forEach { it.asImpl().setUpstreamNode(this) }
    }

    protected fun <T> PushInputChannel<T>.onReceive(
        callback:
            context(Simulator)
            (T) -> Unit
    ) {
        _incoming.add(this)
        this.asImpl().setDownstreamNode(this@Node, callback)
    }

    context(_: Simulator)
    open fun onStart() {}

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

abstract class SourceNode(label: String, outgoing: List<PushOutputChannel<*>>) : Node(label, outgoing) {

    context(_: Simulator)
    abstract override fun onStart()
}

package com.group7

import com.group7.channels.*
import kotlin.time.Duration

abstract class Node(
    label: String,
    final override val incoming: List<InputChannel<*, *>>,
    final override val outgoing: List<OutputChannel<*, *>>,
) : NodeGroup(label) {
    init {
        incoming.forEach { it.setDownstreamNode(this) }
        outgoing.forEach { it.setUpstreamNode(this) }
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

abstract class SourceNode(label: String, outgoing: List<PushOutputChannel<*>>) : Node(label, emptyList(), outgoing) {

    context(_: Simulator)
    abstract override fun onStart()
}

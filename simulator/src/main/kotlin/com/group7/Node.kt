package com.group7

import com.group7.channels.*
import kotlin.time.Duration

/**
 * Base class for nodes in the simulation network.
 *
 * A node is a processing element that receives entities through input channels and sends them through output channels.
 * Nodes form the basic building blocks of a queueing network.
 *
 * @property label human-readable name for the node
 * @property incoming input channels connected to this node
 * @property outgoing output channels connected to this node
 */
abstract class Node(
    label: String,
    final override val incoming: List<InputChannel<*, *>>,
    final override val outgoing: List<OutputChannel<*, *>>,
) : NodeGroup(label) {
    init {
        incoming.forEach { it.setDownstreamNode(this) }
        outgoing.forEach { it.setUpstreamNode(this) }
    }

    /**
     * Called when the simulation starts, before the first event is processed.
     *
     * Subclasses can override this to initialize the node (e.g., schedule initial events).
     */
    context(_: Simulator)
    open fun onStart() {}

    /** Utility methods for scheduling events within a node. */
    protected companion object {
        /**
         * Schedules a callback to execute immediately (with zero delay).
         *
         * @param callback the function to execute
         */
        @JvmStatic
        context(_: Simulator)
        protected fun schedule(callback: () -> Unit) {
            scheduleDelayed(Duration.ZERO, callback)
        }

        /**
         * Schedules a callback to execute after a specified delay.
         *
         * @param delay the simulation time delay before the callback executes
         * @param callback the function to execute
         */
        @JvmStatic
        context(sim: Simulator)
        protected fun scheduleDelayed(delay: Duration, callback: () -> Unit) {
            sim.asImpl().scheduleDelayed(delay, callback)
        }
    }
}

/**
 * Base class for source nodes (nodes with no incoming channels).
 *
 * Source nodes generate entities that enter the simulation network. They must override [onStart] to begin generating
 * entities.
 *
 * @property label human-readable name for the node
 * @property outgoing output channels for sending entities
 */
abstract class SourceNode(label: String, outgoing: List<PushOutputChannel<*>>) : Node(label, emptyList(), outgoing) {

    /** Called when the simulation starts. Must be implemented by subclasses to generate entities. */
    context(_: Simulator)
    abstract override fun onStart()
}

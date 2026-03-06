package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.InputChannel
import com.group7.channels.OutputChannel
import com.group7.properties.Container
import com.group7.utils.andThen

/**
 * Base class for nodes that hold entities. Provides callbacks for entity entry and exit events, allowing tracking of
 * occupancy and behavior throughout the simulation.
 *
 * @param T the type of entities held
 * @param label the name of this node
 * @param incoming the list of input channels
 * @param outgoing the list of output channels
 */
abstract class ContainerNode<T>(
    label: String,
    incoming: List<InputChannel<*, *>>,
    outgoing: List<OutputChannel<*, *>>,
) : Node(label, incoming, outgoing), Container<T> {
    private var enterCallback:
        (context(Simulator)
        (T) -> Unit)? =
        null
    private var leaveCallback:
        (context(Simulator)
        (T) -> Unit)? =
        null

    /**
     * Registers a callback to be invoked when an entity enters this container.
     *
     * @param callback the function to invoke with the entering entity
     */
    override fun onEnter(
        callback:
            context(Simulator)
            (T) -> Unit
    ) {
        enterCallback = enterCallback.andThen(callback)
    }

    /**
     * Registers a callback to be invoked when an entity leaves this container.
     *
     * @param callback the function to invoke with the leaving entity
     */
    override fun onLeave(
        callback:
            context(Simulator)
            (T) -> Unit
    ) {
        leaveCallback = leaveCallback.andThen(callback)
    }

    context(_: Simulator)
    protected fun notifyEnter(obj: T) {
        enterCallback?.let { it(obj) }
    }

    context(_: Simulator)
    protected fun notifyLeave(obj: T) {
        leaveCallback?.let { it(obj) }
    }
}

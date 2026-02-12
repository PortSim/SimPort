package com.group7.nodes

import com.group7.Node
import com.group7.Simulator
import com.group7.channels.InputChannel
import com.group7.channels.OutputChannel
import com.group7.properties.Container
import com.group7.utils.andThen

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

    override fun onEnter(
        callback:
            context(Simulator)
            (T) -> Unit
    ) {
        enterCallback = enterCallback.andThen(callback)
    }

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

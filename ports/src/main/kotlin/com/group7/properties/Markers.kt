package com.group7.properties

import com.group7.Simulator

/*
Contains marker interfaces that describes what callbacks metrics that are interested in tracking the node can attach to
 */

/**
 * Indicates a source node, which can emit entities, representing entities coming from the outside world into the port
 * network
 */
interface Source<out T> {
    /** Callback the source node would execute when emitting an entity */
    fun onEmit(
        callback:
            context(Simulator)
            (T) -> Unit
    )
}

interface Delay<out T> : Container<T>

/**
 * Indicates the node can match two input streams.
 *
 * @property onMatch Assign a callback for when the node performs a match on its inputs
 */
interface Match<out MainInputT, out SideInputT, out OutputT> {
    fun onMatch(
        callback:
            context(Simulator)
            (MainInputT, SideInputT, OutputT) -> Unit
    )
}

/** Indicates a node stores items without processing them */
interface Queue<out T> : Container<T>

/**
 * Indicates a node applies some delay or applies some service to a finite set of entities
 *
 * @property isServing Whether the node is serving some entity
 */
interface Service<out T> : BoundedContainer<T> {
    val isServing: Boolean
}

/** Indicates a sink node that represents lost traffic */
interface LossSink<out T> : Sink<T>

/** Indicates a sink node that represents traffic leaving the port */
interface OutputSink<out T> : Sink<T>

/**
 * Indicates a sink node (an unlimited capacity [Container]) where entities only enter and never leave
 *
 * Nodes with this tag do not support residence time, as entities never leave this container.
 */
interface Sink<out T> : Container<T> {

    // Ignores onLeave, as nothing ever leaves this Container
    override fun onLeave(
        callback:
            context(Simulator)
            (T) -> Unit
    ) {}

    // Node does not support residence time tracking, as things can never leave.
    override fun supportsResidenceTime() = false
}

/**
 * Indicates a node that splits entities into separate entities.
 *
 * @property onSplit Callback to invoke when node performs a splitting action.
 */
interface Split<out InputT, out MainOutputT, out SideOutputT> {
    fun onSplit(
        callback:
            context(Simulator)
            (InputT, MainOutputT, SideOutputT) -> Unit
    )
}

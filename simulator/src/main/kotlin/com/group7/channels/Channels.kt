package com.group7.channels

import com.group7.Node

/** Marker type for distinguishing between push and pull channel semantics. */
sealed interface ChannelType<SelfT : ChannelType<SelfT>> {
    /** Marker for push channels where the sender initiates data transfer. */
    data object Push : ChannelType<Push>

    /** Marker for pull channels where the receiver requests data. */
    data object Pull : ChannelType<Pull>
}

/**
 * Base interface for the input side of a communication channel.
 *
 * The input side connects the upstream [OutputChannel] to the downstream [Node] that receives data.
 *
 * @param ItemT the type of items transmitted through this channel
 * @param ChannelT the channel type (Push or Pull)
 */
sealed interface InputChannel<out ItemT, ChannelT : ChannelType<ChannelT>> {
    /** The output channel connected upstream. */
    val upstream: OutputChannel<*, ChannelT>

    /** The [Node] that receives data through this input channel. */
    val downstreamNode: Node
}

/**
 * Internal function to set the downstream node for an input channel.
 *
 * @param node the [Node] to set as downstream
 */
internal fun InputChannel<*, *>.setDownstreamNode(node: Node) {
    when (this) {
        is PushInputChannelImpl -> this.downstreamNode = node
        is PullInputChannelImpl -> this.downstreamNode = node
    }
}

/**
 * Base interface for the output side of a communication channel.
 *
 * The output side connects the upstream [Node] to the downstream [InputChannel].
 *
 * @param ItemT the type of items transmitted through this channel
 * @param ChannelT the channel type (Push or Pull)
 */
sealed interface OutputChannel<in ItemT, ChannelT : ChannelType<ChannelT>> {
    /** Total number of data transmissions through this channel. */
    val transmissionCount: Int

    /** The input channel connected downstream. */
    val downstream: InputChannel<*, ChannelT>

    /** The [Node] that sends data through this output channel. */
    val upstreamNode: Node
}

/**
 * Internal function to set the upstream node for an output channel.
 *
 * @param node the [Node] to set as upstream
 */
internal fun OutputChannel<*, *>.setUpstreamNode(node: Node) {
    when (this) {
        is PushOutputChannelImpl -> this.upstreamNode = node
        is PullOutputChannelImpl -> this.upstreamNode = node
    }
}

/**
 * Input channel that can be manually connected to an output channel.
 *
 * @param ItemT the type of items transmitted through this channel
 * @param ChannelT the channel type (Push or Pull)
 */
sealed interface ConnectableInputChannel<ItemT, ChannelT : ChannelType<ChannelT>> : InputChannel<ItemT, ChannelT>

/**
 * Output channel that can be manually connected to an input channel.
 *
 * @param ItemT the type of items transmitted through this channel
 * @param ChannelT the channel type (Push or Pull)
 */
sealed interface ConnectableOutputChannel<ItemT, ChannelT : ChannelType<ChannelT>> : OutputChannel<ItemT, ChannelT> {
    /**
     * Connects this output channel to a downstream input channel.
     *
     * @param downstream the [ConnectableInputChannel] to connect to
     */
    fun connectTo(downstream: ConnectableInputChannel<in ItemT, ChannelT>)
}

/**
 * Factory function to create a connectable input channel of the specified type.
 *
 * @param type the [ChannelType] to create (Push or Pull)
 * @return a new connectable input channel
 */
@Suppress("UNCHECKED_CAST")
fun <ItemT, ChannelT : ChannelType<ChannelT>> newConnectableInputChannel(type: ChannelT) =
    when (type) {
        ChannelType.Push -> newConnectablePushInputChannel<ItemT>()
        ChannelType.Pull -> newConnectablePullInputChannel()
    }
        as ConnectableInputChannel<ItemT, ChannelT>

/**
 * Factory function to create a connectable output channel of the specified type.
 *
 * @param type the [ChannelType] to create (Push or Pull)
 * @return a new connectable output channel
 */
@Suppress("UNCHECKED_CAST")
fun <ItemT, ChannelT : ChannelType<ChannelT>> newConnectableOutputChannel(type: ChannelT) =
    when (type) {
        ChannelType.Push -> newConnectablePushOutputChannel<ItemT>()
        ChannelType.Pull -> newConnectablePullOutputChannel()
    }
        as ConnectableOutputChannel<ItemT, ChannelT>

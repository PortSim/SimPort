package com.group7.channels

import com.group7.Node

sealed interface ChannelType<SelfT : ChannelType<SelfT>> {
    data object Push : ChannelType<Push>

    data object Pull : ChannelType<Pull>
}

sealed interface InputChannel<out ItemT, ChannelT : ChannelType<ChannelT>> {
    val upstream: OutputChannel<*, ChannelT>
    val downstreamNode: Node
}

internal fun InputChannel<*, *>.setDownstreamNode(node: Node) {
    when (this) {
        is PushInputChannelImpl -> this.downstreamNode = node
    }
}

sealed interface OutputChannel<in ItemT, ChannelT : ChannelType<ChannelT>> {
    val downstream: InputChannel<*, ChannelT>
    val upstreamNode: Node
}

internal fun OutputChannel<*, *>.setUpstreamNode(node: Node) {
    when (this) {
        is PushOutputChannelImpl -> this.upstreamNode = node
    }
}

sealed interface ConnectableInputChannel<ItemT, ChannelT : ChannelType<ChannelT>> : InputChannel<ItemT, ChannelT>

sealed interface ConnectableOutputChannel<ItemT, ChannelT : ChannelType<ChannelT>> : OutputChannel<ItemT, ChannelT> {
    fun connectTo(downstream: ConnectableInputChannel<in ItemT, ChannelT>)
}

@Suppress("UNCHECKED_CAST")
fun <ItemT, ChannelT : ChannelType<ChannelT>> newConnectableInputChannel(type: ChannelT) =
    when (type) {
        ChannelType.Push -> newConnectablePushInputChannel<ItemT>()
        ChannelType.Pull -> TODO()
    }
        as ConnectableInputChannel<ItemT, ChannelT>

@Suppress("UNCHECKED_CAST")
fun <ItemT, ChannelT : ChannelType<ChannelT>> newConnectableOutputChannel(type: ChannelT) =
    when (type) {
        ChannelType.Push -> newConnectablePushOutputChannel<ItemT>()
        ChannelType.Pull -> TODO()
    }
        as ConnectableOutputChannel<ItemT, ChannelT>

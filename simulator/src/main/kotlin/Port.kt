package com.group7

data class Port(
    val nodes: List<Node<*, *, *>>,
    val channels: List<ChannelImpl<*>>
)
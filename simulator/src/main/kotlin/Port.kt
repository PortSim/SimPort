package com.group7

data class Port(
    val nodes: List<Node<*, *, *>>,
    val inputChannels: List<InputChannel<*>>,
    val outputChannels: List<OutputChannel<*>>,
)

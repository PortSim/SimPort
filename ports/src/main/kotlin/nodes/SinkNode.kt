package com.group7.nodes

import com.group7.Node
import com.group7.channels.PushInputChannel
import com.group7.channels.onReceive
import com.group7.properties.Sink

class SinkNode<InputT>(label: String, source: PushInputChannel<InputT>) :
    Node(label, listOf(source), emptyList()), Sink {
    private val results = mutableMapOf<InputT, Int>()

    override var occupants = 0
        private set

    init {
        source.onReceive {
            results.compute(it) { _, count -> (count ?: 0) + 1 }
            occupants++
        }
    }
}

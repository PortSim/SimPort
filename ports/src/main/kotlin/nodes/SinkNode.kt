package com.group7.nodes

import com.group7.InputChannel
import com.group7.Metrics
import com.group7.Node
import com.group7.properties.Sink

class SinkNode<InputT>(label: String, inputChannel: InputChannel<InputT>) : Node(label, emptyList()), Sink {
    private val results = mutableMapOf<InputT, Int>()

    override var occupants = 0
        private set

    init {
        inputChannel.onReceive {
            results.compute(it) { _, count -> (count ?: 0) + 1 }
            occupants++
        }
    }

    override fun reportMetrics(): Metrics {
        return Metrics(occupants = occupants)
    }
}

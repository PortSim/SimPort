package com.group7.nodes

import com.group7.InputChannel
import com.group7.Metrics
import com.group7.Node

open class SinkNode<InputT>(label: String, inputChannel: InputChannel<InputT>) : Node(label, emptyList()) {
    private val results = mutableMapOf<InputT, Int>()
    private var count = 0

    init {
        inputChannel.onReceive {
            results.compute(it) { _, count -> (count ?: 0) + 1 }
            count++
        }
    }

    override fun reportMetrics(): Metrics {
        return Metrics(occupants = count)
    }
}

package com.group7

open class Sink<InputT>(label: String, incoming: List<InputChannel<InputT>>) : Node(label) {
    private val results = mutableMapOf<InputT, Int>()
    private var count = 0

    init {
        for (channel in incoming) {
            channel.onReceive {
                results.compute(it) { _, count -> (count ?: 0) + 1 }
                count++
            }
        }
    }

    override fun reportMetrics(): Metrics {
        return Metrics(null, count)
    }
}

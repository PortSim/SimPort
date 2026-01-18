package com.group7.nodes

import com.group7.InputChannel
import com.group7.Metrics
import com.group7.Node
import com.group7.OutputChannel
import com.group7.generators.DelayProvider

class DelayNode<T>(
    label: String,
    source: InputChannel<T>,
    destination: OutputChannel<T>,
    delayProvider: DelayProvider,
) : Node(label, listOf(destination)) {

    private var occupants = 0

    init {
        source.onReceive {
            occupants++
            scheduleDelayed(delayProvider.nextDelay()) {
                destination.send(it)
                occupants--
            }
        }
    }

    override fun reportMetrics() = Metrics(occupants = occupants)
}

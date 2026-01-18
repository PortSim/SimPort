package com.group7.nodes

import com.group7.InputChannel
import com.group7.Metrics
import com.group7.Node
import com.group7.OutputChannel
import com.group7.Simulator

class QueueNode<T>(
    label: String,
    source: InputChannel<T>,
    private val destination: OutputChannel<T>,
    initialContents: List<T> = emptyList(),
) : Node(label, listOf(destination)) {

    private val contents = ArrayDeque(initialContents)
    private var scheduled = false

    init {
        source.onReceive { onArrive(it) }
        destination.whenOpened { scheduleDrain() }
    }

    context(_: Simulator)
    override fun onStart() {
        scheduleDrain()
    }

    override fun reportMetrics() = Metrics(occupants = contents.size)

    context(_: Simulator)
    private fun onArrive(obj: T) {
        contents.addLast(obj)
        scheduleDrain()
    }

    context(_: Simulator)
    private fun drain() {
        while (contents.isNotEmpty() && destination.isOpen()) {
            destination.send(contents.removeFirst())
        }
    }

    context(_: Simulator)
    private fun scheduleDrain() {
        if (scheduled || contents.isEmpty()) {
            return
        }
        schedule {
            drain()
            scheduled = false
        }
        scheduled = true
    }
}

package com.group7.nodes

import com.group7.*
import com.group7.policies.queue.FIFOQueuePolicy
import com.group7.policies.queue.QueuePolicy

class QueueNode<T>(
    label: String,
    source: InputChannel<T>,
    private val destination: OutputChannel<T>,
    private val policy: QueuePolicy<T> = FIFOQueuePolicy(),
) : Node(label, listOf(destination)) {

    private var scheduled = false

    init {
        source.onReceive { onArrive(it) }
        destination.whenOpened { scheduleDrain() }
    }

    context(_: Simulator)
    override fun onStart() {
        scheduleDrain()
    }

    override fun reportMetrics() = Metrics(occupants = policy.reportOccupancy())

    context(_: Simulator)
    private fun onArrive(obj: T) {
        policy.enqueue(obj)
        scheduleDrain()
    }

    context(_: Simulator)
    private fun drain() {
        while (policy.reportOccupancy() > 0 && destination.isOpen()) {
            destination.send(policy.dequeue())
        }
    }

    context(_: Simulator)
    private fun scheduleDrain() {
        if (scheduled || policy.reportOccupancy() == 0) {
            return
        }
        schedule {
            drain()
            scheduled = false
        }
        scheduled = true
    }
}

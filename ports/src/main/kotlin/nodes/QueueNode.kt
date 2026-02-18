package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.*
import com.group7.policies.queue.FIFOQueuePolicy
import com.group7.policies.queue.QueuePolicy
import com.group7.properties.Queue

class QueueNode<T>(
    label: String,
    source: PushInputChannel<T>,
    private val destination: PullOutputChannel<T>,
    private val policy: QueuePolicy<T> = FIFOQueuePolicy(),
) : ContainerNode<T>(label, listOf(source), listOf(destination)), Queue<T> {

    init {
        source.onReceive { onArrive(it) }
        destination.onPull { emit() }
    }

    context(_: Simulator)
    override fun onStart() {
        if (policy.reportOccupancy() > 0) {
            destination.markReady()
        }
        for (initialOccupant in policy.contents) {
            notifyEnter(initialOccupant)
        }
    }

    override val occupants
        get() = policy.reportOccupancy()

    override fun supportsResidenceTime() = policy.supportsResidenceTime()

    context(_: Simulator)
    private fun onArrive(obj: T) {
        policy.enqueue(obj)
        notifyEnter(obj)
        destination.markReady()
    }

    context(_: Simulator)
    private fun emit(): T {
        val result = policy.dequeue()
        notifyLeave(result)
        if (policy.reportOccupancy() == 0) {
            destination.markNotReady()
        }
        return result
    }
}

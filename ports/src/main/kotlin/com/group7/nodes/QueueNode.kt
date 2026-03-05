package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.*
import com.group7.policies.queue.FIFOQueuePolicy
import com.group7.policies.queue.QueuePolicy
import com.group7.properties.Queue

/**
 * A queue that receives entities through a push channel and outputs them through a pull channel. Entities are stored
 * and ordered according to the provided [QueuePolicy] (defaults to FIFO). The queue remains open to receive entities
 * and signals readiness when it contains items.
 *
 * @param T the type of entities in the queue
 * @param label the name of this node
 * @param source the input channel from which entities are received
 * @param destination the output channel where entities are sent
 * @param policy the [QueuePolicy] that determines entity ordering (defaults to FIFO)
 */
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

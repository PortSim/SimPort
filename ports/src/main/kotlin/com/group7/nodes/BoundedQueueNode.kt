package com.group7.nodes

import com.group7.Simulator
import com.group7.channels.*
import com.group7.policies.queue.FIFOQueuePolicy
import com.group7.policies.queue.QueuePolicy
import com.group7.properties.BoundedContainer
import com.group7.properties.Queue

/**
 * A queue with a maximum capacity. Receives entities through a push channel and outputs through a pull channel.
 *
 * When the queue reaches capacity, the input channel closes to prevent further arrivals.
 *
 * Uses the provided [QueuePolicy] to order entities (defaults to FIFO).
 *
 * @param T the type of entities in the queue
 * @param label the name of this node
 * @param source the input channel from which entities are received
 * @param destination the output channel where entities are sent
 * @param capacity the maximum number of entities the queue can hold
 * @param policy the [QueuePolicy] that determines entity ordering (defaults to FIFO)
 * @property occupants the current number of entities in the queue
 */
class BoundedQueueNode<T>(
    label: String,
    private val source: PushInputChannel<T>,
    private val destination: PullOutputChannel<T>,
    override val capacity: Int,
    private val policy: QueuePolicy<T> = FIFOQueuePolicy(),
) : ContainerNode<T>(label, listOf(source), listOf(destination)), Queue<T>, BoundedContainer<T> {

    init {
        source.onReceive { onArrive(it) }
        destination.onPull { emit() }
        require(capacity >= policy.reportOccupancy()) {
            "Bounded queue is starting with ${policy.reportOccupancy()} occupants, but capacity is only $capacity"
        }
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
        if (policy.reportOccupancy() >= capacity) {
            source.close()
        }
    }

    context(_: Simulator)
    private fun emit(): T {
        val result = policy.dequeue()
        notifyLeave(result)
        source.open()
        if (policy.reportOccupancy() == 0) {
            destination.markNotReady()
        }
        return result
    }
}

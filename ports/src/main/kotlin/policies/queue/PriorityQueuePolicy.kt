package com.group7.policies.queue

import java.util.*

/**
 * Priority queue policy. Entities are dequeued according to a priority order defined by the [comparator]. Lower
 * priority entities (earlier in the comparator order) are dequeued first.
 *
 * @param T the type of entities in the queue
 * @param initialContents the initial entities in the queue
 * @param comparator the [Comparator] that defines the priority order
 */
class PriorityQueuePolicy<T : Any>(
    initialContents: Collection<T> = emptyList(),
    private val comparator: Comparator<T>,
) : QueuePolicy<T> {
    val pq =
        PriorityQueue<T>(initialContents.size.coerceAtLeast(1), comparator.reversed()).apply { addAll(initialContents) }

    override val contents
        get() = pq.asSequence().sortedWith(comparator)

    override fun enqueue(obj: T) {
        pq.add(obj)
    }

    override fun dequeue(): T {
        return pq.remove()
    }

    override fun reportOccupancy(): Int {
        return pq.size
    }
}

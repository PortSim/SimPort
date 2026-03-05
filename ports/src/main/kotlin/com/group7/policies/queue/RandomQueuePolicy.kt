package com.group7.policies.queue

import com.group7.utils.RandomContext

/**
 * Random queue policy. Entities are dequeued randomly from the queue.
 *
 * @param T the type of entities in the queue
 * @param initialContents the initial entities in the queue
 */
class RandomQueuePolicy<T>(initialContents: Collection<T> = emptyList()) : QueuePolicy<T> {
    private val random = RandomContext.newRandom()
    private val _contents = initialContents.toMutableList()

    override val contents
        get() = _contents.asSequence()

    override fun enqueue(obj: T) {
        _contents.add(obj)
    }

    override fun dequeue(): T {
        val i = _contents.indices.random(random)
        if (i == _contents.lastIndex) {
            return _contents.removeLast()
        }
        return _contents[i].also { _contents[i] = _contents.removeLast() }
    }

    override fun reportOccupancy(): Int {
        return _contents.size
    }
}

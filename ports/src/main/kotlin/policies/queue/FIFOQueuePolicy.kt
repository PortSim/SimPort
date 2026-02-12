package com.group7.policies.queue

class FIFOQueuePolicy<T>(initialContents: Collection<T> = emptyList()) : QueuePolicy<T> {
    private val fifo = ArrayDeque(initialContents)

    override val contents
        get() = fifo.asSequence()

    override fun enqueue(obj: T) {
        fifo.addFirst(obj)
    }

    override fun dequeue(): T {
        return fifo.removeLast()
    }

    override fun reportOccupancy(): Int {
        return fifo.size
    }
}

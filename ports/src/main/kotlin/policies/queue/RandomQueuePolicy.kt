package com.group7.policies.queue

class RandomQueuePolicy<T>(initialContents: Collection<T> = emptyList()) : QueuePolicy<T> {
    private val contents = initialContents.toMutableList()

    override fun enqueue(obj: T) {
        contents.add(obj)
    }

    override fun dequeue(): T {
        val i = contents.indices.random()
        if (i == contents.lastIndex) {
            return contents.removeLast()
        }
        return contents[i].also { contents[i] = contents.removeLast() }
    }

    override fun reportOccupancy(): Int {
        return contents.size
    }
}

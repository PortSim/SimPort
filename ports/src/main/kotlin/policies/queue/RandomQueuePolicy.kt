package com.group7.policies.queue

class RandomQueuePolicy<T>(initialContents: Collection<T> = emptyList()) : QueuePolicy<T> {
    private val _contents = initialContents.toMutableList()

    override val contents
        get() = _contents.asSequence()

    override fun enqueue(obj: T) {
        _contents.add(obj)
    }

    override fun dequeue(): T {
        val i = _contents.indices.random()
        if (i == _contents.lastIndex) {
            return _contents.removeLast()
        }
        return _contents[i].also { _contents[i] = _contents.removeLast() }
    }

    override fun reportOccupancy(): Int {
        return _contents.size
    }
}

package com.group7.policies.queue

interface QueuePolicy<T> {
    val contents: Sequence<T>

    fun enqueue(obj: T)

    fun dequeue(): T

    fun reportOccupancy(): Int

    fun supportsResidenceTime(): Boolean = true
}

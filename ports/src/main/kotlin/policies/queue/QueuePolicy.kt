package com.group7.policies.queue

interface QueuePolicy<T> {
    fun enqueue(obj: T)

    fun dequeue(): T

    fun reportOccupancy(): Int
}

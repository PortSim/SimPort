package com.group7.policies.queue

/**
 * Policy for managing queue behavior. Determines how entities are stored and retrieved from a queue. Implementations
 * can provide different ordering strategies (FIFO, priority, random, etc.).
 *
 * @param T the type of entities in the queue
 */
interface QueuePolicy<T> {
    /** A sequence of all entities currently in the queue, in their stored order. */
    val contents: Sequence<T>

    /**
     * Adds an entity to the queue according to the policy.
     *
     * @param obj the entity to add
     */
    fun enqueue(obj: T)

    /**
     * Removes and returns an entity from the queue according to the policy.
     *
     * @return the next entity to remove
     */
    fun dequeue(): T

    /**
     * Reports the current number of entities in the queue.
     *
     * @return the occupancy count
     */
    fun reportOccupancy(): Int

    /**
     * Indicates whether this policy supports residence time tracking for entities.
     *
     * @return true if residence time tracking is supported, false otherwise
     */
    fun supportsResidenceTime(): Boolean = true
}

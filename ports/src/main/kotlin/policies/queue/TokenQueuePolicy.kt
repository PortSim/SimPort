package com.group7.policies.queue

/**
 * Sentinel object representing an abstract token. Used in token-based queuing systems for capacity control or access
 * management.
 */
data object Token

/**
 * Token queue policy. Manages a pool of abstract tokens used for flow control. Rather than storing actual entities,
 * tracks token availability. Does not support residence time tracking since tokens are not individual entities.
 *
 * @param count the initial number of tokens available
 */
class TokenQueuePolicy(private var count: Int) : QueuePolicy<Token> {
    override val contents
        get() = generateSequence { Token }.take(count)

    override fun enqueue(obj: Token) {
        count++
    }

    override fun dequeue(): Token {
        check(count >= 0) { "Queue reached negative token count" }
        if (count == 0) {
            throw NoSuchElementException("The queue is empty")
        }

        count--
        return Token
    }

    override fun reportOccupancy() = count

    override fun supportsResidenceTime() = false
}

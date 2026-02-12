package com.group7.policies.queue

data object Token

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
}

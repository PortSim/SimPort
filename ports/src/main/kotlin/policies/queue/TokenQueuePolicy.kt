package com.group7.policies.queue

data object Token

class TokenQueuePolicy(private var count: Int) : QueuePolicy<Token> {
    override fun enqueue(obj: Token) {
        count++
    }

    override fun dequeue(): Token {
        check(count > 0) { "Queue is empty" }
        count--
        return Token
    }

    override fun reportOccupancy() = count
}

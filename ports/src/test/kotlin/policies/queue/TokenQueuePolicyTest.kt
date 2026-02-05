package com.group7.policies.queue

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TokenQueuePolicyTest :
    FunSpec({
        test("TokenQueuePolicy returns token when dequeued") {
            val tokenPolicy = TokenQueuePolicy(10)

            for (i in 1..10) {
                tokenPolicy.dequeue() shouldBe Token
            }
        }

        test("TokenQueuePolicy throws when dequeued while empty") {
            val tokenPolicy = TokenQueuePolicy(1)

            tokenPolicy.dequeue() shouldBe Token
            shouldThrow<NoSuchElementException> { tokenPolicy.dequeue() }
        }
    })

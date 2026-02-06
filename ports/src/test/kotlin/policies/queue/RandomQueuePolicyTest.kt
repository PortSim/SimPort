package com.group7.policies.queue

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class RandomQueuePolicyTest :
    FunSpec({
        test("RandomQueuePolicy dequeues all elements exactly once in any order") {
            val randomPolicy = RandomQueuePolicy<Int>()

            val input = (1..10).toList()
            input.forEach { randomPolicy.enqueue(it) }

            val results = mutableListOf<Int>()
            repeat(input.size) { results.add(randomPolicy.dequeue()) }

            results.size shouldBe input.size
            results shouldContainExactlyInAnyOrder input
        }

        test("Priority policy throws when dequeued while empty") {
            val randomPolicy = RandomQueuePolicy<Int>()

            randomPolicy.enqueue(1)

            randomPolicy.dequeue() shouldBe 1

            shouldThrow<NoSuchElementException> { randomPolicy.dequeue() }
        }
    })

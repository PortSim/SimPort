package com.group7.policies.queue

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PriorityQueuePolicyTest :
    FunSpec({
        test("PriorityQueuePolicy respects priority order") {
            val priorityPolicy = PriorityQueuePolicy<Int>(comparator = compareBy { it })

            priorityPolicy.enqueue(5)
            priorityPolicy.enqueue(4)
            priorityPolicy.enqueue(2)

            priorityPolicy.dequeue() shouldBe 5
            priorityPolicy.dequeue() shouldBe 4

            priorityPolicy.enqueue(3)
            priorityPolicy.enqueue(1)

            priorityPolicy.dequeue() shouldBe 3
            priorityPolicy.dequeue() shouldBe 2
            priorityPolicy.dequeue() shouldBe 1
        }

        test("PriorityQueuePolicy throws when dequeued while empty") {
            val priorityPolicy = PriorityQueuePolicy<Int>(comparator = compareBy { it })

            priorityPolicy.enqueue(1)

            priorityPolicy.dequeue() shouldBe 1

            shouldThrow<NoSuchElementException> { priorityPolicy.dequeue() }
        }
    })

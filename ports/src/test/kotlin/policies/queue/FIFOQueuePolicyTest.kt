package com.group7.policies.queue

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FIFOQueuePolicyTest :
    FunSpec({
        test("FIFOQueuePolicy preserves FIFO order") {
            val fifoPolicy = FIFOQueuePolicy<Int>()

            fifoPolicy.enqueue(1)
            fifoPolicy.enqueue(2)
            fifoPolicy.enqueue(3)

            fifoPolicy.dequeue() shouldBe 1
            fifoPolicy.dequeue() shouldBe 2

            fifoPolicy.enqueue(4)
            fifoPolicy.dequeue() shouldBe 3
            fifoPolicy.dequeue() shouldBe 4
        }

        test("FIFOQueuePolicy throws when dequeued while empty") {
            val fifoPolicy = FIFOQueuePolicy<Int>()

            fifoPolicy.enqueue(1)

            fifoPolicy.dequeue() shouldBe 1

            shouldThrow<NoSuchElementException> { fifoPolicy.dequeue() }
        }
    })

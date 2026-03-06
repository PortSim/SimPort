package com.group7.state

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.floats.shouldBeBetween
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ProgressBarTest :
    FunSpec({
        val start = Instant.fromEpochSeconds(1000)
        val end = start + 10.seconds

        fun bar(seq: Long = 1, startTime: Instant = start, endTime: Instant = end) =
            ProgressBar("test", startTime, endTime, seq)

        context("duration") {
            test("returns difference between end and start") { bar().duration() shouldBe 10.seconds }

            test("zero-length bar") { ProgressBar("z", start, start, 1).duration() shouldBe 0.seconds }
        }

        context("proportionCompleted") {
            test("at start time returns 0") { bar().proportionCompleted(start) shouldBe 0f }

            test("at end time returns 1") { bar().proportionCompleted(end).shouldBeBetween(0.99f, 1.01f, 0f) }

            test("halfway through returns 0.5") {
                bar().proportionCompleted(start + 5.seconds).shouldBeBetween(0.49f, 0.51f, 0f)
            }

            test("before start time is clamped to 0") { bar().proportionCompleted(start - 5.seconds) shouldBe 0f }

            test("after end time exceeds 1") {
                val result = bar().proportionCompleted(end + 5.seconds)
                result shouldBeGreaterThan 1f
            }
        }

        context("shouldShow") {
            test("true before end time") { bar().shouldShow(start + 5.seconds) shouldBe true }

            test("false at end time") { bar().shouldShow(end) shouldBe false }

            test("false after end time") { bar().shouldShow(end + 1.seconds) shouldBe false }
        }

        context("compareTo") {
            test("earlier end time sorts first") {
                val early = bar(seq = 1, endTime = start + 5.seconds)
                val late = bar(seq = 2, endTime = start + 10.seconds)
                early shouldBeLessThan late
            }

            test("same end time sorts by sequence number") {
                val first = bar(seq = 1)
                val second = bar(seq = 2)
                first shouldBeLessThan second
            }
        }

        context("equals and hashCode") {
            test("same sequence number means equal") {
                val a = ProgressBar("a", start, end, 42)
                val b = ProgressBar("b", start, end + 1.minutes, 42)
                a shouldBe b
                a.hashCode() shouldBe b.hashCode()
            }

            test("different sequence numbers are not equal") {
                val a = bar(seq = 1)
                val b = bar(seq = 2)
                (a == b) shouldBe false
            }
        }
    })

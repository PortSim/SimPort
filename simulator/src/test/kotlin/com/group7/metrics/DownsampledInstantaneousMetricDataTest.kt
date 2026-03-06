package com.group7.metrics

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlin.time.Instant

private fun timeAt(i: Int): Instant = Instant.fromEpochMilliseconds(i.toLong())

class DownsampledInstantaneousMetricDataTest :
    FunSpec({
        test("empty state has no values") {
            val data = DownsampledInstantaneousMetricData()
            data.values.shouldBeEmpty()
        }

        test("single sample is stored") {
            val data = DownsampledInstantaneousMetricData()
            data.add(timeAt(100), 42.0)
            data.values.size shouldBeExactly 1
            data.values[0].time shouldBe timeAt(100)
            data.values[0].value shouldBe 42.0
        }

        test("samples below capacity are all retained") {
            val data = DownsampledInstantaneousMetricData()
            val n = 100
            for (i in 0 until n) {
                data.add(timeAt(i), i.toDouble())
            }
            data.values.size shouldBeExactly n
        }

        test("values are returned sorted by time") {
            val data = DownsampledInstantaneousMetricData()
            // Add samples in reverse time order
            for (i in 99 downTo 0) {
                data.add(timeAt(i), i.toDouble())
            }
            data.values.shouldBeSortedWith(compareBy { it.time })
        }

        test("duplicate timestamps are preserved via sequenceId tie-breaker") {
            val data = DownsampledInstantaneousMetricData()
            val sameTime = timeAt(42)
            data.add(sameTime, 1.0)
            data.add(sameTime, 2.0)
            data.add(sameTime, 3.0)
            data.values.size shouldBeExactly 3
            data.values.map { it.value } shouldContainAll listOf(1.0, 2.0, 3.0)
        }

        test("exactly DESIRED_SAMPLES fills reservoir without eviction") {
            val data = DownsampledInstantaneousMetricData()
            for (i in 0 until DownsampledInstantaneousMetricData.DESIRED_SAMPLES) {
                data.add(timeAt(i), i.toDouble())
            }
            data.values.size shouldBeExactly DownsampledInstantaneousMetricData.DESIRED_SAMPLES
        }

        test("reservoir caps at DESIRED_SAMPLES after many additions") {
            val data = DownsampledInstantaneousMetricData()
            val totalSamples = DownsampledInstantaneousMetricData.DESIRED_SAMPLES * 3
            for (i in 0 until totalSamples) {
                data.add(timeAt(i), i.toDouble())
            }
            data.values.size shouldBeExactly DownsampledInstantaneousMetricData.DESIRED_SAMPLES
        }

        test("values remain sorted after reservoir evictions") {
            val data = DownsampledInstantaneousMetricData()
            val totalSamples = DownsampledInstantaneousMetricData.DESIRED_SAMPLES * 2
            for (i in 0 until totalSamples) {
                data.add(timeAt(i), i.toDouble())
            }
            data.values.shouldBeSortedWith(compareBy { it.time })
        }

        test("first and last points have a chance of surviving reservoir sampling") {
            val totalSamples = DownsampledInstantaneousMetricData.DESIRED_SAMPLES * 2
            val survived =
                (1..20).any {
                    val data = DownsampledInstantaneousMetricData()
                    for (i in 0 until totalSamples) {
                        data.add(timeAt(i), i.toDouble())
                    }
                    val values = data.values
                    values.any { it.value == 0.0 } || values.any { it.value == (totalSamples - 1).toDouble() }
                }
            survived shouldBe true
        }

        test("time-value pairs remain consistent after evictions") {
            val data = DownsampledInstantaneousMetricData()
            for (i in 0 until DownsampledInstantaneousMetricData.DESIRED_SAMPLES + 3000) {
                // value = time * 10, so we can verify pairing
                data.add(timeAt(i), i * 10.0)
            }
            for (mv in data.values) {
                val expectedValue = mv.time.toEpochMilliseconds() * 10.0
                mv.value shouldBe expectedValue
            }
        }

        test("interleaved timestamps maintain sort order") {
            val data = DownsampledInstantaneousMetricData()
            // Add timestamps in an interleaved pattern
            for (i in 0 until 200) {
                data.add(timeAt(i * 3), i.toDouble()) // 0, 3, 6, 9, ...
                data.add(timeAt(i * 3 + 2), i.toDouble()) // 2, 5, 8, 11, ...
                data.add(timeAt(i * 3 + 1), i.toDouble()) // 1, 4, 7, 10, ...
            }
            data.values.shouldBeSortedWith(compareBy { it.time })
        }
    })
